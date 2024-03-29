/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cebitec.mgx.configuration.api;

import java.io.File;
import java.net.URI;

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

    File getPersistentDirectory();

    File getPluginDump();

    int getSQLBulkInsertSize();

    public URI getAnnotationService();

}
