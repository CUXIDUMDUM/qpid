#!/usr/bin/env ruby
# Usage: output_directory xml_spec_file [xml_spec_file...]
# 
$: << '..'
require 'cppgen'

class StructGen < CppGen

  def initialize(outdir, amqp)
    super(outdir, amqp)
  end

  EncodingMap={
    "octet"=>"Octet",
    "short"=>"Short",
    "long"=>"Long",
    "longlong"=>"LongLong",
    "longstr"=>"LongString",
    "shortstr"=>"ShortString",
    "timestamp"=>"LongLong",
    "table"=>"FieldTable",
    "content"=>"Content",
    "long-struct"=>"LongString"
  }
  SizeMap={
    "octet"=>1,
    "short"=>2,
    "long"=>4,
    "longlong"=>8,
    "timestamp"=>8
  }

  ValueTypes=["octet", "short", "long", "longlong", "timestamp"]

  def is_packed(s)
    s.kind_of? AmqpStruct
  end

  def execution_header?(s)
    false and s.kind_of? AmqpMethod and s.parent.l4?
  end

  def has_bitfields_only(s)
    s.fields.select {|f| f.domain.type_ != "bit"}.empty?
  end

  def default_initialisation(s)
    params = s.fields.select {|f| ValueTypes.include?(f.domain.type_) || f.domain.type_ == "bit"}
    strings = params.collect {|f| "#{f.cppname}(0)"}   
    if strings.empty?
      return ""
    else
      return " : " + strings.join(", ")
    end
  end

  def printable_form(f)
    if (f.cpptype.name == "uint8_t")
      return "(int) " + f.cppname
    else
      return f.cppname
    end
  end

  def flag_mask(s, i)
    pos = SizeMap[s.pack]*8 - 8 - (i/8)*8 + (i % 8)
    return "(1 << #{pos})"
  end

  def get_flags_impl(s)
    genl "#{s.cpp_pack_type.name} flags = 0;"
    process_packed_fields(s) { |f, i| set_field_flag(s, f, i) }
    genl "return flags;"
  end

  def set_field_flag(s, f, i)
    if (ValueTypes.include?(f.domain.type_) || f.domain.type_ == "bit")
      genl "if (#{f.cppname}) flags |= #{flag_mask(s, i)};"
    else
      genl "if (#{f.cppname}.size()) flags |= #{flag_mask(s, i)};"
    end
  end

  def encode_packed_struct(s)
    genl "#{s.cpp_pack_type.name} flags = getFlags();"
    genl s.cpp_pack_type.encode('flags', 'buffer')
    process_packed_fields(s) { |f, i| encode_packed_field(s, f, i) unless f.domain.type_ == "bit" }
  end

  def decode_packed_struct(s)
    genl "#{s.cpp_pack_type.name} #{s.cpp_pack_type.decode('flags', 'buffer')}"
    process_packed_fields(s) { |f, i| decode_packed_field(s, f, i) unless f.domain.type_ == "bit" }
    process_packed_fields(s) { |f, i| set_bitfield(s, f, i) if f.domain.type_ == "bit" }
  end

  def size_packed_struct(s)
    genl "#{s.cpp_pack_type.name} flags = getFlags();" unless has_bitfields_only(s)
    genl "total += #{SizeMap[s.pack]};"
    process_packed_fields(s) { |f, i| size_packed_field(s, f, i) unless f.domain.type_ == "bit" }
  end

  def encode_packed_field(s, f, i)
    genl "if (flags & #{flag_mask(s, i)})"
    indent { genl f.domain.cpptype.encode(f.cppname,"buffer") }
  end

  def decode_packed_field(s, f, i)
    genl "if (flags & #{flag_mask(s, i)})"
    indent { genl f.domain.cpptype.decode(f.cppname,"buffer") }
  end

  def size_packed_field(s, f, i)
      genl "if (flags & #{flag_mask(s, i)})"
      indent { generate_size(f, []) }
  end

  def set_bitfield(s, f, i)
      genl "#{f.cppname} = (flags & #{flag_mask(s, i)});"
  end

  def generate_encode(f, combined)
    if (f.domain.type_ == "bit")
      genl "uint8_t #{f.cppname}_bits = #{f.cppname};"
      count = 0
      combined.each { |c| genl "#{f.cppname}_bits |= #{c.cppname} << #{count += 1};" }
      genl "buffer.putOctet(#{f.cppname}_bits);"
    else
      genl f.domain.cpptype.encode(f.cppname,"buffer")
    end
  end

  def generate_decode(f, combined)
    if (f.domain.type_ == "bit")
      genl "uint8_t #{f.cppname}_bits = buffer.getOctet();"
      genl "#{f.cppname} = 1 & #{f.cppname}_bits;"
      count = 0
      combined.each { |c| genl "#{c.cppname} = (1 << #{count += 1}) & #{f.cppname}_bits;" }
    else
      genl f.domain.cpptype.decode(f.cppname,"buffer")
    end
  end

  def generate_size(f, combined)
    if (f.domain.type_ == "bit")
      names = ([f] + combined).collect {|g| g.cppname}
      genl "total += 1;//#{names.join(", ")}"
    else
      size = SizeMap[f.domain.type_]
      if (size)
        genl "total += #{size};//#{f.cppname}"
      elsif (f.cpptype.name == "SequenceNumberSet")
        genl "total += #{f.cppname}.encodedSize();"
      else 
        encoded = EncodingMap[f.domain.type_]        
        gen "total += ("
        gen "4 " if encoded == "LongString"
        gen "1 " if encoded == "ShortString"
        genl "+ #{f.cppname}.size());"
      end
    end
  end

  def process_packed_fields(s)
    s.fields.each { |f| yield f, s.fields.index(f) }
  end

  def process_fields(s)
    last = nil
    count = 0  
    bits = []
    s.fields.each { 
      |f| if (last and last.bit? and f.bit? and count < 7) 
            count += 1
            bits << f
          else
            if (last and last.bit?)
              yield last, bits
              count = 0
              bits = []
            end
            if (not f.bit?)
              yield f
            end
            last = f
          end
    }
    if (last and last.bit?)
      yield last, bits
    end
  end

  def methodbody_extra_defs(s)
    gen <<EOS
    using  AMQMethodBody::accept;
    void accept(MethodBodyConstVisitor& v) const { v.visit(*this); }

    inline ClassId amqpClassId() const { return CLASS_ID; }
    inline MethodId amqpMethodId() const { return METHOD_ID; }
    inline bool isContentBearing() const { return  #{s.content ? "true" : "false" }; }
    inline bool resultExpected() const { return  #{s.result ? "true" : "false"}; }
    inline bool responseExpected() const { return  #{s.responses().empty? ? "false" : "true"}; }
EOS
  end

  def define_constructor(name, s)
    if (s.fields.size > 0)
      genl "#{name}("
      if (s.kind_of? AmqpMethod)
        indent {gen "ProtocolVersion, "}
      end
      indent { gen s.fields.collect { |f| "#{f.cpptype.param} _#{f.cppname}" }.join(",\n") }
      gen ")"
      genl ": " if s.fields.size > 0
      indent { gen s.fields.collect { |f| " #{f.cppname}(_#{f.cppname})" }.join(",\n") }
      genl " {}"
    end
    if (s.kind_of? AmqpMethod)
      genl "#{name}(ProtocolVersion=ProtocolVersion()) {}"
    end
    if (s.kind_of? AmqpStruct)
      genl "#{name}() #{default_initialisation(s)} {}"
    end
  end

  def define_accessors(f)
    genl "void set#{f.name.caps}(#{f.cpptype.param} _#{f.cppname}) { #{f.cppname} = _#{f.cppname}; }"
    genl "#{f.cpptype.ret} get#{f.name.caps}() const { return #{f.cppname}; }"
    if (f.cpptype.name == "FieldTable")
      genl "#{f.cpptype.name}& get#{f.name.caps}() { return #{f.cppname}; }"
    end
  end

  def define_struct(s)
    classname = s.cppname
    inheritance = ""
    if (s.kind_of? AmqpMethod)
      classname = s.body_name
      if (execution_header?(s))
        inheritance = ": public ModelMethod"
      else
        inheritance = ": public AMQMethodBody"
      end
    end

    h_file("qpid/framing/#{classname}.h") { 
      if (s.kind_of? AmqpMethod)
        gen <<EOS
#include "qpid/framing/AMQMethodBody.h"
#include "qpid/framing/AMQP_ServerOperations.h"
#include "qpid/framing/MethodBodyConstVisitor.h"
EOS
      end
      include "qpid/framing/ModelMethod.h" if (execution_header?(s))

      #need to include any nested struct definitions
      s.fields.each { |f| include f.cpptype.name if f.domain.struct }

      gen <<EOS

#include <ostream>
#include "qpid/framing/amqp_types_full.h"

namespace qpid {
namespace framing {

class #{classname} #{inheritance} {
EOS
  indent { s.fields.each { |f| genl "#{f.cpptype.name} #{f.cppname};" } }
  if (is_packed(s))
    indent { genl "#{s.cpp_pack_type.name} getFlags() const;"}
  end
  genl "public:"
  if (s.kind_of? AmqpMethod)
    indent { genl "static const ClassId CLASS_ID = #{s.parent.index};" }
    indent { genl "static const MethodId METHOD_ID = #{s.index};" }
  end

  if (s.kind_of? AmqpStruct)
    if (s.type_)
      if (s.result?)
        #as result structs have types that are only unique to the
        #class, they have a class dependent qualifier added to them
        #(this is inline with current python code but a formal
        #solution is expected from the WG)
        indent { genl "static const uint16_t TYPE = #{s.type_} + #{s.parent.parent.parent.index} * 256;" }
      else
        indent { genl "static const uint16_t TYPE = #{s.type_};" } 
      end
    end
  end

  indent { 
    define_constructor(classname, s)
    genl ""
    s.fields.each { |f| define_accessors(f) } 
  }
  if (s.kind_of? AmqpMethod)
    methodbody_extra_defs(s)
  end
  if (s.kind_of? AmqpStruct)
    indent {genl "friend std::ostream& operator<<(std::ostream&, const #{classname}&);" }
  end

  gen <<EOS
    void encode(Buffer&) const;
    void decode(Buffer&, uint32_t=0);
    uint32_t size() const;
    void print(std::ostream& out) const;
}; /* class #{classname} */

}}
EOS
    }
    cpp_file("qpid/framing/#{classname}.cpp") { 
      if (s.fields.size > 0 || execution_header?(s))
        buffer = "buffer"
      else
        buffer = "/*buffer*/"
      end
      gen <<EOS
#include "#{classname}.h"

using namespace qpid::framing;

EOS
    
      if (is_packed(s))
        genl "#{s.cpp_pack_type.name} #{classname}::getFlags() const"
        genl "{"
        indent { get_flags_impl(s) }
        genl "}"
      end
      gen <<EOS
void #{classname}::encode(Buffer& #{buffer}) const
{
EOS
      if (execution_header?(s))
        genl "ModelMethod::encode(buffer);"
      end

      if (is_packed(s))
        indent {encode_packed_struct(s)}
      else 
        indent { process_fields(s) { |f, combined| generate_encode(f, combined) } } 
      end
      gen <<EOS
}

void #{classname}::decode(Buffer& #{buffer}, uint32_t /*size*/)
{
EOS
      if (execution_header?(s))
        genl "ModelMethod::decode(buffer);"
      end

      if (is_packed(s))
        indent {decode_packed_struct(s)}
      else 
        indent { process_fields(s) { |f, combined| generate_decode(f, combined) } } 
      end
      gen <<EOS
}

uint32_t #{classname}::size() const
{
    uint32_t total = 0;
EOS
      if (execution_header?(s))
        genl "total += ModelMethod::size();"
      end

      if (is_packed(s))
        indent {size_packed_struct(s)}
      else 
        indent { process_fields(s) { |f, combined| generate_size(f, combined) } } 
      end
      gen <<EOS
    return total;
}

void #{classname}::print(std::ostream& out) const
{
    out << "#{classname}: ";
EOS
      copy = Array.new(s.fields)
      f = copy.shift
  
      indent { 
        genl "out << \"#{f.name}=\" << #{printable_form(f)};" if f
        copy.each { |f| genl "out << \"; #{f.name}=\" << #{printable_form(f)};" } 
      } 
      gen <<EOS
}
EOS

      if (s.kind_of? AmqpStruct)
        gen <<EOS
namespace qpid{
namespace framing{

    std::ostream& operator<<(std::ostream& out, const #{classname}& s) 
    {
      s.print(out);
      return out;
    }

}
}
EOS
      end 
}
  end

  def generate()
    @amqp.structs.each { |s| define_struct(s) }
    @amqp.methods_.each { |m| define_struct(m) }
    #generate a single include file containing the list of structs for convenience
    h_file("qpid/framing/amqp_structs.h") { @amqp.structs.each { |s| genl "#include \"#{s.cppname}.h\"" } }
  end
end

StructGen.new(ARGV[0], Amqp).generate()

