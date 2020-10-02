package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Description of class BootParameters.
 * set the boot parameters configuration file
 * fetch boot parameters.
 */
public class BootParameters {

    /**
     * This method used to fetch boot parameters data.
     * @return boot parameters data
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    PropertyDocument getBootParameters() throws SimulatorException {
        try {
            return processData(readConfigFile(bootParamsConfigFile_));
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given boot parameters config file doesn't exists : " + bootParamsConfigFile_);
        } catch (final ConfigIOParseException | IOException e) {
            throw new SimulatorException("Error in loading boot parameters data.");
        }
    }

    /**
     * This method used to fetch boot parameters data for a location.
     * @return boot parameters data for a location
     * @throws SimulatorException when unable to find configuration file or process data.
     */
    PropertyDocument getBootParametersForLocation(String location) throws SimulatorException {
        try {
            return processDataForLocation(processData(readConfigFile(bootParamsConfigFile_)), location);
        } catch (final FileNotFoundException e) {
            throw new SimulatorException("Given boot parameters config file doesn't exists : " + bootParamsConfigFile_);
        } catch (final ConfigIOParseException | IOException | PropertyNotExpectedType e) {
            throw new SimulatorException("Error in loading boot parameters data.");
        }
    }

    /**
     * This method used to set boot parameters configuration file.
     * @param bootParamsConfigFile location of boot parameters configuration file.
     * @throws SimulatorException when unable to set the location of boot parameters configuration file.
     */
    void setBootParamsConfigFile(final String bootParamsConfigFile) throws SimulatorException {
        if (bootParamsConfigFile == null || bootParamsConfigFile.isEmpty())
            throw new SimulatorException("Invalid or null boot parameters config file.");
        bootParamsConfigFile_ = bootParamsConfigFile;
    }

    /**
     * This method process the boot parameters configuration file data.
     * @param data boot parameters configuration file data.
     * @return processed boot parameters configuration file data.
     * @throws SimulatorException when unable to boot parameters configuration file data.
     */
    private PropertyArray processData(PropertyDocument data) throws SimulatorException {
        if (data == null || !data.isArray() || data.getAsArray().isEmpty())
            throw new SimulatorException("No boot-parameters data.");
        return data.getAsArray();
    }

    /**
     * This method process the boot parameters configuration file data.
     * @param data boot parameters configuration file data.
     * @param location location for which data needs to be retrieved.
     * @return processed boot parameters configuration file data.
     * @throws PropertyNotExpectedType when unable to fetch hosts information data.
     */
    private PropertyArray processDataForLocation(PropertyArray data, String location) throws PropertyNotExpectedType {
        PropertyArray result = new PropertyArray();
        for(int i = 0; i < data.size(); i++) {
            PropertyMap bootParamsData = data.getMap(i);
            if(bootParamsData.getArrayOrDefault("hosts", new PropertyArray()).contains(location)) {
                result.clear();
                result.add(bootParamsData);
                break;
            }
        }
        return result;
    }

    /**
     * This method reads the boot parameters configuration file.
     * @param bootParametersConfigFile boot parameters configuration file.
     * @return file data.
     * @throws IOException  unable to find file or parse data.
     * @throws ConfigIOParseException unable to find file or parse data.
     */
    private PropertyDocument readConfigFile(String bootParametersConfigFile) throws IOException, ConfigIOParseException {
        try {
            return LoadFileLocation.fromFileLocation(bootParametersConfigFile).getAsArray();
        } catch (FileNotFoundException e) {
            return LoadFileLocation.fromResources(bootParametersConfigFile).getAsArray();
        }
    }

    private String bootParamsConfigFile_;
}