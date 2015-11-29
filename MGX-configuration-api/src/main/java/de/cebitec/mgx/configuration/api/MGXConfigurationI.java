/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.configuration.api;

import java.io.File;

/**
 *
 * @author sjaenick
 */
public interface MGXConfigurationI {

    /*
     * settings for the MGX global zone
     */
    String getMGXGlobalStorageDir();

    String getMGXPassword();

    String getMGXUser();

    String getPersistentDirectory();

    File getPluginDump();

    int getSQLBulkInsertSize();

    /*
     * settings specific to project database
     */
    int getTransferTimeout();
    
}
