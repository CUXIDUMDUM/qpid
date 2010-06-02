/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 *
 */

package org.apache.qpid.server.logging;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import java.io.File;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

public class GenerateLogMessages
{
    private static String _tmplDir;
    private String _outputDir;
    private String _resource;

    public static void main(String[] args)
    {
        GenerateLogMessages generator = null;
        try
        {
            generator = new GenerateLogMessages(args);
        }
        catch (IllegalAccessException iae)
        {
            // This occurs when args does not contain Template and output dirs.
            System.exit(-1);
        }
        catch (Exception e)
        {
            //This is thrown by the Velocity Engine initialisation
            e.printStackTrace();
            System.exit(-1);
        }

        try
        {
            generator.run();
        }
        catch (InvalidTypeException e)
        {
            // This occurs when a type other than 'number' appears in the
            // paramater config {0, number...}
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    GenerateLogMessages(String[] args) throws Exception
    {
        processArgs(args);

        // We need the template and output dirs set to run.
        if (_tmplDir == null || _outputDir == null)
        {
            showUsage();
            throw new IllegalAccessException();
        }

        // Initialise the Velocity Engine, Telling it where our macro lives
        Properties props = new Properties();
        props.setProperty("file.resource.loader.path", _tmplDir);
        Velocity.init(props);
    }

    private void showUsage()
    {
        System.out.println("Broker LogMessageGenerator v.0.0");
        System.out.println("Usage: GenerateLogMessages: -t tmplDir");
        System.out.println("       where -t tmplDir: Find templates in tmplDir.");
        System.out.println("             -o outDir:  Use outDir as the output dir.");
        System.out.println("             -r resource:  The resource to build.");
    }

    public void run() throws InvalidTypeException, Exception
    {

        Map<String,String> messageClasses = extractMessageClasses();

        for (String key : messageClasses.keySet())
        {
            createMessageClass(messageClasses.get(key),key);
        }
    }

    private Map<String, String> extractMessageClasses()
    {

        ResourceBundle _messages = ResourceBundle.getBundle(_resource, Locale.US);

        Enumeration<String> messageKeys = _messages.getKeys();

        Map<String,String> types = new HashMap<String, String>();

        //Identify 3-Digit keys
        while(messageKeys.hasMoreElements())
        {
            String key = messageKeys.nextElement();

            if (key.length() == 3)
            {
                types.put(key,_messages.getString(key));
            }
        }

        return types;
    }

    /**
     * Process the args for:
     *   -t|T value for the template location
     *   -o|O value for the output directory
     *
     * @param args the commandline arguments
     */
    private void processArgs(String[] args)
    {
        // Crude but simple...
        for (int i = 0; i < args.length; i++)
        {
            String arg = args[i];
            if (arg.charAt(0) == '-')
            {
                switch (arg.charAt(1))
                {
                    case 'o':
                    case 'O':
                        if (++i < args.length)
                        {
                            _outputDir = args[i];
                        }
                        break;
                    case 't':
                    case 'T':
                        if (++i < args.length)
                        {
                            _tmplDir = args[i];
                        }
                        break;
                    case 'r':
                    case 'R':
                        if (++i < args.length)
                        {
                            _resource = args[i];
                        }
                }
            }
        }
    }

    /**
     * This is the main method that generates the _Messages.java file.
     * The class is generated by extracting the list of messges from the
     * available LogMessages Resource.
     *
     * The extraction is done based on typeIdentifier which is a 3-digit prefix
     * on the messages e.g. BRK for Broker.
     *
     * @param className The name for the file '_className_Messages.java'
     * @param typeIdentifier The 3 digit identifier
     * @throws InvalidTypeException when an unknown parameter type is used in the properties file
     * @throws Exception thrown by velocity if there is an error
     */
    private void createMessageClass(String className, String typeIdentifier)
            throws InvalidTypeException, Exception
    {
        VelocityContext context = new VelocityContext();

        // Get the Data for this class and typeIdentifier
        HashMap<String, Object> typeData = prepareType(className, typeIdentifier);


        context.put("package", _resource.substring(0, _resource.lastIndexOf(".")));
        //Store the resource Bundle name for the macro
        context.put("resource",_resource);

        // Store this data in the context for the macro to access
        context.put("type", typeData);

        // Create the file writer to put the finished file in
        FileWriter output = new FileWriter(_outputDir + File.separator + className + "Messages.java");

        // Run Velocity to create the output file.
        // Fix the default file encoding to 'ISO-8859-1' rather than let
        // Velocity fix it. This is the encoding format for the macro.
        Velocity.mergeTemplate("LogMessages.vm", "ISO-8859-1", context, output);

        //Close our file.
        output.flush();
        output.close();
    }

    /**
     * This method does the processing and preparation of the data to be added
     * to the Velocity context so that the macro can access and process the data
     *
     * The given messageKey (e.g. 'BRK') uses a 3-digit code used to match
     * the property key from the loaded 'LogMessages' ResourceBundle.
     *
     * This gives a list of messages which are to be associated with the given
     * messageName (e.g. 'Broker')
     *
     * Each of the messages in the list are then processed to identify how many
     * parameters the MessageFormat call is expecting. These parameters are
     * identified by braces ('{}') so a quick count of these can then be used
     * to create a defined parameter list.
     *
     * Rather than defaulting all parameters to String a check is performed to
     * see if a 'number' value has been requested. e.g. {0. number}
     * {@see MessageFormat}. If a parameter has a 'number' type then the
     * parameter will be defined as an Number value. This allows for better
     * type checking during compilation whilst allowing the log message to
     * maintain formatting controls.
     *
     * OPTIONS
     *
     * The returned hashMap contains the following structured data:
     *
     * - name - ClassName ('Broker')
     * - list[logEntryData] - methodName ('BRK_1001')
     *                      - name ('BRK-1001')
     *                      - format ('Startup : Version: {0} Build: {1}')
     *                      - parameters (list)
     *                          - type ('String'|'Number')
     *                          - name ('param1')
     *                      - options (list)
     *                          - name ('opt1')
     *                          - value ('AutoDelete')
     *
     * @param messsageName the name to give the Class e.g. 'Broker'
     * @param messageKey the 3-digit key to extract the messages e.g. 'BRK'
     * @return A HashMap with data for the macro
     * @throws InvalidTypeException when an unknown parameter type is used in the properties file
     */
    private HashMap<String, Object> prepareType(String messsageName, String messageKey) throws InvalidTypeException
    {
        // Load the LogMessages Resource Bundle
        ResourceBundle _messages = ResourceBundle.getBundle(_resource, Locale.US);

        Enumeration<String> messageKeys = _messages.getKeys();

        //Create the return map
        HashMap<String, Object> messageTypeData = new HashMap<String, Object>();
        // Store the name to give to this Class <name>Messages.java
        messageTypeData.put("name", messsageName);

        // Prepare the list of log messages
        List<HashMap> logMessageList = new LinkedList<HashMap>();
        messageTypeData.put("list", logMessageList);

        //Process each of the properties
        while (messageKeys.hasMoreElements())
        {
            HashMap<String, Object> logEntryData = new HashMap<String, Object>();

            //Add MessageName to amp
            String message = messageKeys.nextElement();

            // Process the log message if it matches the specified key e.g.'BRK_'
            if (message.startsWith(messageKey+"_"))
            {
                // Method names can't have a '-' in them so lets make it '_'
                // e.g. BRK-STARTUP -> BRK_STARTUP
                logEntryData.put("methodName", message.replace('-', '_'));
                // Store the real name so we can use that in the actual log.
                logEntryData.put("name", message);

                //Retrieve the actual log message string.
                String logMessage = _messages.getString(message);

                // Store the value of the property so we can add it to the
                // Javadoc of the method.
                logEntryData.put("format", logMessage);

                // Add the parameters for this message
                logEntryData.put("parameters", extractParameters(logMessage));

                //Add the options for this messagse
                logEntryData.put("options", extractOptions(logMessage));

                //Add this entry to the list for this class
                logMessageList.add(logEntryData);
            }
        }

        return messageTypeData;
    }

    /**
     * This method is responsible for extracting the options form the given log
     * message and providing a HashMap with the expected data:
     *                      - options (list)
     *                          - name ('opt1')
     *                          - value ('AutoDelete')
     *
     * The options list that this method provides must contain HashMaps that
     * have two entries. A 'name' and a 'value' these strings are important as
     * they are used in LogMessage.vm to extract the stored String during
     * processing of the template.
     *
     * The 'name' is a unique string that is used to name the boolean parameter
     * and refer to it later in the method body.
     *
     * The 'value' is used to provide documentation in the generated class to
     * aid readability.
     *
     * @param logMessage the message from the properties file
     * @return list of option data
     */
    private List<HashMap<String, String>> extractOptions(String logMessage)
    {
        // Split the string on the start brace '{' this will give us the
        // details for each parameter that this message contains.
        // NOTE: Simply splitting on '[' prevents the use of nested options.
        // Currently there is no demand for nested options        
        String[] optionString = logMessage.split("\\[");
        // Taking an example of:
        // 'Text {n,type,format} [option] text {m} [option with param{p}] more'
        // This would give us:
        // 0 - Text {n,type,format}
        // 1 - option] text {m}
        // 2 - option with param{p}] more

        // Create the parameter list for this item
        List<HashMap<String, String>> options = new LinkedList<HashMap<String, String>>();

        // Check that we have some parameters to process
        // Skip 0 as that will not be the first entry
        //  Text {n,type,format} [option] text {m} [option with param{p}] more
        if (optionString.length > 1)
        {
            for (int index = 1; index < optionString.length; index++)
            {
                // Use a HashMap to store the type,name of the parameter
                // for easy retrieval in the macro template
                HashMap<String, String> option = new HashMap<String, String>();

                // Locate the end of the Option
                // NOTE: it is this simple matching on the first ']' that
                // prevents the nesting of options.
                // Currently there is no demand for nested options
                int end = optionString[index].indexOf("]");

                // The parameter type
                String value = optionString[index].substring(0, end);

                // Simply name the parameters by index.
                option.put("name", "opt" + index);

                //Store the value e.g. AutoDelete
                // We trim as we don't need to include any whitespace that is
                // used for formating. This is only used to aid readability of
                // of the generated code.
                option.put("value", value.trim());

                options.add(option);
            }
        }

        return options;
    }

    /**
     * This method is responsible for extract the parameters that are requried
     * for this log message. The data is returned in a HashMap that has the
     * following structure:
     *                      - parameters (list)
     *                          - type ('String'|'Number')
     *                          - name ('param1')
     *
     * The parameters list that is provided must contain HashMaps that have the
     * two named entries. A 'type' and a 'name' these strings are importans as
     * they are used in LogMessage.vm to extract and the stored String during
     * processing of the template
     *
     * The 'type' and 'name' values are used to populate the method signature.
     * This is what gives us the compile time validation of log messages.
     *
     * The 'name' must be unique as there may be many parameters. The value is
     * also used in the method body to pass the values through to the
     * MessageFormat class for formating the log message.
     *
     * @param logMessage the message from the properties file
     * @return list of option data
     * @throws InvalidTypeException if the FormatType is specified and is not 'number'
     */
    private List<HashMap<String, String>> extractParameters(String logMessage)
            throws InvalidTypeException
    {
        // Split the string on the start brace '{' this will give us the
        // details for each parameter that this message contains.
        String[] parametersString = logMessage.split("\\{");
        // Taking an example of 'Text {n[,type]} text {m} more text {p}'
        // This would give us:
        // 0 - Text
        // 1 - n[,type]} text
        // 2 - m} more text
        // 3 - p}

        // Create the parameter list for this item
        List<HashMap<String, String>> parameters = new LinkedList<HashMap<String, String>>();

        // Check that we have some parameters to process
        // Skip 0 as that will not be the first entry
        //  Text {n[,type]} text {m} more text {p}
        if (parametersString.length > 1)
        {
            for (int index = 1; index < parametersString.length; index++)
            {
                // Use a HashMap to store the type,name of the parameter
                // for easy retrieval in the macro template
                HashMap<String, String> parameter = new HashMap<String, String>();

                // Check for any properties of the parameter :
                // e.g. {0} vs {0,number} vs {0,number,xxxx}
                int typeIndex = parametersString[index].indexOf(",");


                // The parameter type
                String type;

                //Be default all types are Strings
                if (typeIndex == -1)
                {
                    type = "String";
                }
                else
                {
                    //Check string ',....}' for existence of number
                    // to identify this parameter as an integer
                    // This allows for a style value to be present
                    // Only check the text inside the braces '{}'
                    int typeIndexEnd = parametersString[index].indexOf("}", typeIndex);
                    String typeString = parametersString[index].substring(typeIndex, typeIndexEnd);
                    if (typeString.contains("number"))
                    {
                        type = "Number";
                    }
                    else
                    {
                        throw new InvalidTypeException("Invalid type(" + typeString + ") index (" + parameter.size() + ") in message:" + logMessage);
                    }

                }

                //Store the data
                parameter.put("type", type);
                // Simply name the parameters by index.
                parameter.put("name", "param" + index);

                parameters.add(parameter);
            }
        }

        return parameters;
    }

    /**
     * Just a inner exception to be able to identify when a type that is not
     * 'number' occurs in the message parameter text.
     */
    private class InvalidTypeException extends Throwable
    {
        public InvalidTypeException(String message)
        {
            super(message);
        }
    }
}
