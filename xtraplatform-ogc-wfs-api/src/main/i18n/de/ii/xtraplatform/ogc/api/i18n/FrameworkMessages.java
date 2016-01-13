/**
 * Copyright 2016 interactive instruments GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.ii.xtraplatform.ogc.api.i18n;

import org.forgerock.i18n.LocalizableMessageDescriptor;

/**
 * This file contains localizable message descriptors having the resource
 * name {@code de.ii.xtraplatform.ogc.api.i18n.framework}. This file was generated
 * automatically by the {@code i18n-maven-plugin} from the property file
 * {@code de/ii/xtraplatform/ogc/api/i18n/framework.properties} and it should not be manually edited.
 */
public final class FrameworkMessages {
    // The name of the resource bundle.
    private static final String RESOURCE = "de.ii.xtraplatform.ogc.api.i18n.framework";

    // Prevent instantiation.
    private FrameworkMessages() {
        // Do nothing.
    }

    /**
     * Returns the name of the resource associated with the messages contained
     * in this class. The resource name may be used for obtaining named loggers,
     * e.g. using SLF4J's {@code org.slf4j.LoggerFactory#getLogger(String name)}.
     *
     * @return The name of the resource associated with the messages contained
     *         in this class.
     */
    public static String resourceName() {
        return RESOURCE;
    }

    /**
     * Added Default-Namespace: %s, %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ADDED_DEFAULT_NAMESPACE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ADDED_DEFAULT_NAMESPACE", -1);

    /**
     * added default SRS '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ADDED_DEFAULT_SRS =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ADDED_DEFAULT_SRS", -1);

    /**
     * Added gml Namespace: %s, %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ADDED_GML_NAMESPACE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ADDED_GML_NAMESPACE", -1);

    /**
     * Added Namespace: %s, %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ADDED_NAMESPACE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ADDED_NAMESPACE", -1);

    /**
     * added SRS '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ADDED_SRS =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ADDED_SRS", -1);

    /**
     * Adding WFS2GSFS service with id '%s'. WFS-URL: '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ADDING_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ADDING_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL", -1);

    /**
     * analyze Feature Start '%s' '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ANALYZE_FEATURE_START =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ANALYZE_FEATURE_START", -1);

    /**
     * Analyzing Capabilities
     */
    public static final LocalizableMessageDescriptor.Arg0 ANALYZING_CAPABILITIES =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "ANALYZING_CAPABILITIES", -1);

    /**
     * "Analyzing Capabilities (version: %s)
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ANALYZING_CAPABILITIES_VERSION =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ANALYZING_CAPABILITIES_VERSION", -1);

    /**
     * Analyzing WFS
     */
    public static final LocalizableMessageDescriptor.Arg0 ANALYZING_WFS =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "ANALYZING_WFS", -1);

    /**
     * A column with name '%s' does not exist
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> A_COLUMN_WITH_NAME_DOES_NOT_EXIST =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "A_COLUMN_WITH_NAME_DOES_NOT_EXIST", -1);

    /**
     * A feature with id '%s' is not available for this layer.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> A_FEATURE_WITH_ID_ID_IS_NOT_AVAILABLE_FOR_THIS_LAYER =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "A_FEATURE_WITH_ID_ID_IS_NOT_AVAILABLE_FOR_THIS_LAYER", -1);

    /**
     * A layer with ID '%s' is not available for this service.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> A_LAYER_WITH_ID_IS_NOT_AVAILABLE_FOR_THIS_SERVICE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "A_LAYER_WITH_ID_IS_NOT_AVAILABLE_FOR_THIS_SERVICE", -1);

    /**
     * Can't write index.json for Layer templates
     */
    public static final LocalizableMessageDescriptor.Arg0 CANT_WRITE_INDEX_JSON_FOR_LAYER_TEMPLATES =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "CANT_WRITE_INDEX_JSON_FOR_LAYER_TEMPLATES", -1);

    /**
     * Created WFS2GSFS service with id '%s' successfully. WFS-URL: '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> CREATED_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "CREATED_WFS2GSFS_SERVICE_WITH_ID_ID_WFSURL_URL", -1);

    /**
     * %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> DEBUG =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "DEBUG", -1);

    /**
     * Error deleting service '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_DELETING_SERVICE_ID =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_DELETING_SERVICE_ID", -1);

    /**
     * Error in POST request to URL: %s
     * Request: %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ERROR_IN_POST_REQUEST_TO_URL_REQUEST =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ERROR_IN_POST_REQUEST_TO_URL_REQUEST", -1);

    /**
     * Error loading resource: %s %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> ERROR_LOADING_RESOURCE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "ERROR_LOADING_RESOURCE", -1);

    /**
     * Error loading resource overwrite: %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_LOADING_RESOURCE_OVERWRITE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_LOADING_RESOURCE_OVERWRITE", -1);

    /**
     * Error parsing application schema. %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_PARSING_APPLICATION_SCHEMA =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_APPLICATION_SCHEMA", -1);

    /**
     * The GML application schema provided by the WFS imports schema '%s', but that schema cannot be accessed. XtraProxy requires valid GML application schemas to determine the layers of the GeoServices REST API Feature Service and their characteristics. Please contact the WFS provider to correct the schema error.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_PARSING_APPLICATION_SCHEMA_FILE_NOT_FOUND =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_APPLICATION_SCHEMA_FILE_NOT_FOUND", -1);

    /**
     * The GML application schema provided by the WFS is not well-formed. XtraProxy requires valid GML application schemas to determine the layers of the GeoServices REST API Feature Service and their characteristics. Please contact the WFS provider to correct the schema error. %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_PARSING_APPLICATION_SCHEMA_PARSE_EXCEPTION =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_APPLICATION_SCHEMA_PARSE_EXCEPTION", -1);

    /**
     * Error parsing GetFeature request!
     */
    public static final LocalizableMessageDescriptor.Arg0 ERROR_PARSING_GETFEATURE_REQUEST =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_GETFEATURE_REQUEST", -1);

    /**
     * Error parsing GetFeature response
     */
    public static final LocalizableMessageDescriptor.Arg0 ERROR_PARSING_GETFEATURE_RESPONSE =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_GETFEATURE_RESPONSE", -1);

    /**
     * The capabilities document provided by the WFS is invalid. XtraProxy requires valid capabilities document to understand the characteristics of the WFS and access it properly. Please contact the WFS provider to correct the capabilities document of the WFS.
     */
    public static final LocalizableMessageDescriptor.Arg0 ERROR_PARSING_WFS_CAPABILITIES =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_WFS_CAPABILITIES", -1);

    /**
     * Error parsing WFS GetFeature (IOException) %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_PARSING_WFS_GETFEATURE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_PARSING_WFS_GETFEATURE", -1);

    /**
     * Error resolving missing namespace prefixes
     */
    public static final LocalizableMessageDescriptor.Arg0 ERROR_RESOLVING_MISSING_NAMESPACE_PREFIXES =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "ERROR_RESOLVING_MISSING_NAMESPACE_PREFIXES", -1);

    /**
     * Error saving service '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_SAVING_SERVICE_ID =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_SAVING_SERVICE_ID", -1);

    /**
     * Error updating service '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_UPDATING_SERVICE_ID =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_UPDATING_SERVICE_ID", -1);

    /**
     * "Error writing response. (client gone?) %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_WRITING_RESPONSE_CLIENT_GONE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_WRITING_RESPONSE_CLIENT_GONE", -1);

    /**
     * Error writing response. (writeGeometry) %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> ERROR_WRITING_RESPONSE_WRITEGEOMETRY =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "ERROR_WRITING_RESPONSE_WRITEGEOMETRY", -1);

    /**
     * Error writing response. (writeMappedField) %s %s %s
     */
    public static final LocalizableMessageDescriptor.Arg3<Object, Object, Object> ERROR_WRITING_RESPONSE_WRITEMAPPEDFIELD =
                    new LocalizableMessageDescriptor.Arg3<Object, Object, Object>(FrameworkMessages.class, RESOURCE, "ERROR_WRITING_RESPONSE_WRITEMAPPEDFIELD", -1);

    /**
     * Exception: %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> EXCEPTION =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "EXCEPTION", -1);

    /**
     * Exception coming from WFS: '%s'.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> EXCEPTION_COMING_FROM_WFS =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "EXCEPTION_COMING_FROM_WFS", -1);

    /**
     * Failed requesting URL: '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FAILED_REQUESTING_URL =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FAILED_REQUESTING_URL", -1);

    /**
     * Failed requesting URL: '%s' Reason: '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> FAILED_REQUESTING_URL_REASON =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "FAILED_REQUESTING_URL_REASON", -1);

    /**
     * Found data in layer '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FOUND_DATA_IN_LAYER =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FOUND_DATA_IN_LAYER", -1);

    /**
     * Found default NS to resolve missing namespace prefixes: %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FOUND_DEFAULT_NS_TO_RESOLVE_MISSING_NAMESPACE_PREFIXES =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FOUND_DEFAULT_NS_TO_RESOLVE_MISSING_NAMESPACE_PREFIXES", -1);

    /**
     * Found FeatureType with name '%s' in the schema, but not in the expected namespace.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FOUND_FEATURETYPE_NAME_BUT_THE_NAMESPACES_ARE_DIFFERENT =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FOUND_FEATURETYPE_NAME_BUT_THE_NAMESPACES_ARE_DIFFERENT", -1);

    /**
     * Found GmlId '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FOUND_GMLID =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FOUND_GMLID", -1);

    /**
     * Found layer. id: '%s' name: %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> FOUND_LAYER_ID_NAME =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "FOUND_LAYER_ID_NAME", -1);

    /**
     * Found a schema for an alternative namespace '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FOUND_SCHEMA_FOR_ALTERNATIVE_NAMESPACE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FOUND_SCHEMA_FOR_ALTERNATIVE_NAMESPACE", -1);

    /**
     * Found template file for FeatureType '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FOUND_TEMPLATE_FILE_FOR_FEATURETYPE_NAME =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FOUND_TEMPLATE_FILE_FOR_FEATURETYPE_NAME", -1);

    /**
     * FS request received: '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> FS_REQUEST_RECEIVED =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "FS_REQUEST_RECEIVED", -1);

    /**
     * GET Request %s: %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> GET_REQUEST_OPERATION_URL =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "GET_REQUEST_OPERATION_URL", -1);

    /**
     * GET request timed out after %d ms, URL: %s
     */
    public static final LocalizableMessageDescriptor.Arg2<Number, Object> GET_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST =
                    new LocalizableMessageDescriptor.Arg2<Number, Object>(FrameworkMessages.class, RESOURCE, "GET_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST", -1);

    /**
     * GMLParser recieved 'stop parsing' %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> GMLPARSER_RECIEVED_STOP_PARSING =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "GMLPARSER_RECIEVED_STOP_PARSING", -1);

    /**
     * Invalid WFS url '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> INVALID_WFS_URL =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "INVALID_WFS_URL", -1);

    /**
     * Layertemplate for Featuretype '%s' in Namespace '%s' not found.
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> LAYERTEMPLATE_FOR_NAME_NAMESPACE_NOT_FOUND =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "LAYERTEMPLATE_FOR_NAME_NAMESPACE_NOT_FOUND", -1);

    /**
     * Layer '%s' disabled
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> LAYER_NAME_DISABLED =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "LAYER_NAME_DISABLED", -1);

    /**
     * Layer '%s' enabled
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> LAYER_NAME_ENABLED =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "LAYER_NAME_ENABLED", -1);

    /**
     * Mapped geometry property of type '%s' to geometry field of type '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> MAPPED_GEOMETRY_PROPERTY_OF_TYPE_TO_GEOMETRY_FIELD_OF_TYPE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "MAPPED_GEOMETRY_PROPERTY_OF_TYPE_TO_GEOMETRY_FIELD_OF_TYPE", -1);

    /**
     * Mapped multiple property of type '%s' to field '%s' of type '%s'
     */
    public static final LocalizableMessageDescriptor.Arg3<Object, Object, Object> MAPPED_MULTIPLE_PROPERTY_TYPE_TO_FIELD_OF_TYPE =
                    new LocalizableMessageDescriptor.Arg3<Object, Object, Object>(FrameworkMessages.class, RESOURCE, "MAPPED_MULTIPLE_PROPERTY_TYPE_TO_FIELD_OF_TYPE", -1);

    /**
     * Mapped property of type '%s' to field '%s' of type '%s'
     */
    public static final LocalizableMessageDescriptor.Arg3<Object, Object, Object> MAPPED_PROPERTY_OF_TYPETO_FIELD_OF_TYPE =
                    new LocalizableMessageDescriptor.Arg3<Object, Object, Object>(FrameworkMessages.class, RESOURCE, "MAPPED_PROPERTY_OF_TYPETO_FIELD_OF_TYPE", -1);

    /**
     * Mapping FeatureType '%s' to Layer '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> MAPPING_FEATURETYPE_TO_LAYER =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "MAPPING_FEATURETYPE_TO_LAYER", -1);

    /**
     * Missing gml:id/fid for features in layer '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> MISSING_GML_ID_FID_FOR_FEATURES_IN_LAYER =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "MISSING_GML_ID_FID_FOR_FEATURES_IN_LAYER", -1);

    /**
     * This module allows to create GSFS services that use WFS as data source and automatically derive a mapping for the data model.
     */
    public static final LocalizableMessageDescriptor.Arg0 MODULE_DESCRIPTION =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "MODULE_DESCRIPTION", -1);

    /**
     * No data in layer '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> NO_DATA_IN_LAYER_NAME =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "NO_DATA_IN_LAYER_NAME", -1);

    /**
     * No valid SRS found in GetCapabilities response
     */
    public static final LocalizableMessageDescriptor.Arg0 NO_VALID_SRS_FOUND_IN_GETCAPABILITIES_RESPONSE =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "NO_VALID_SRS_FOUND_IN_GETCAPABILITIES_RESPONSE", -1);

    /**
     * NSURI is EMPTY
     */
    public static final LocalizableMessageDescriptor.Arg0 NSURI_IS_EMPTY =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "NSURI_IS_EMPTY", -1);

    /**
     * NumberFormatException in writeMappedField type: '%s' recieved value: '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> NUMBERFORMATEXCEPTION_WRITEMAPPEDFIELD =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "NUMBERFORMATEXCEPTION_WRITEMAPPEDFIELD", -1);

    /**
     * Parsing Capabilities for WFS
     */
    public static final LocalizableMessageDescriptor.Arg0 PARSING_CAPABILITIES_FOR_WFS =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "PARSING_CAPABILITIES_FOR_WFS", -1);

    /**
     * Parsing GetFeature response for '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> PARSING_GETFEATURE_RESPONSE_FOR =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "PARSING_GETFEATURE_RESPONSE_FOR", -1);

    /**
     * Parsing SQL: %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> PARSING_SQL =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "PARSING_SQL", -1);

    /**
     * POST request timed out after %d ms, URL: %s 
     * Request: %s
     */
    public static final LocalizableMessageDescriptor.Arg3<Number, Object, Object> POST_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST =
                    new LocalizableMessageDescriptor.Arg3<Number, Object, Object>(FrameworkMessages.class, RESOURCE, "POST_REQUEST_TIMED_OUT_AFTER_MS_URL_REQUEST", -1);

    /**
     * Reading layer template '%s' failed
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> READING_LAYER_TEMPLATE_FILENAME_FAILED =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "READING_LAYER_TEMPLATE_FILENAME_FAILED", -1);

    /**
     * Reading template for FeatureType '%s' failed
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> READING_TEMPLATE_FOR_FEATURETYPE_NAME_FAILED =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "READING_TEMPLATE_FOR_FEATURETYPE_NAME_FAILED", -1);

    /**
     * Removing URL: %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> REMOVING_URL =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "REMOVING_URL", -1);

    /**
     * Replacing the namespace for layer '%s', using '%s' instead '%s'.
     */
    public static final LocalizableMessageDescriptor.Arg3<Object, Object, Object> REPLACING_THE_CAPABILITIES_NAMESPACE_FOR_FEATURETYPE_WITH =
                    new LocalizableMessageDescriptor.Arg3<Object, Object, Object>(FrameworkMessages.class, RESOURCE, "REPLACING_THE_CAPABILITIES_NAMESPACE_FOR_FEATURETYPE_WITH", -1);

    /**
     * Required parameter '%s' is missing in request
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> REQUIRED_PARAMETER_PARAMNAME_IS_MISSING_IN_REQUEST =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "REQUIRED_PARAMETER_PARAMNAME_IS_MISSING_IN_REQUEST", -1);

    /**
     * Retry with default URL: %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> RETRY_WITH_DEFAULT_URL =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "RETRY_WITH_DEFAULT_URL", -1);

    /**
     * Save as Template failed. A Layer with id '%s' is not available.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> SAVE_AS_TEMPLATE_FAILED_A_LAYER_WITH_ID_IS_NOT_AVAILABLE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "SAVE_AS_TEMPLATE_FAILED_A_LAYER_WITH_ID_IS_NOT_AVAILABLE", -1);

    /**
     * Saving layer template for FeatureType '%s:%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> SAVING_LAYER_TEMPLATE_FOR_FEATURETYPE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "SAVING_LAYER_TEMPLATE_FOR_FEATURETYPE", -1);

    /**
     * "saving layer template to '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> SAVING_LAYER_TEMPLATE_TO_FILENAME =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "SAVING_LAYER_TEMPLATE_TO_FILENAME", -1);

    /**
     * Saving layer to '%s' failed
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> SAVING_LAYER_TO_FILENAME_FAILED =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "SAVING_LAYER_TO_FILENAME_FAILED", -1);

    /**
     * Schema for Namespace '%s' not found
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> SCHEMA_FOR_NAMESPACE_NOT_FOUND =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "SCHEMA_FOR_NAMESPACE_NOT_FOUND", -1);

    /**
     * Schema for Namespace '%s' not found, searching in targetNamespace schema instead. 
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> SCHEMA_FOR_NAMESPACE_NOT_FOUND_RETRYING =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "SCHEMA_FOR_NAMESPACE_NOT_FOUND_RETRYING", -1);

    /**
     * You reached the maximum number of services for this organization. If you want to add a new service, you have to delete an existing one first.
     */
    public static final LocalizableMessageDescriptor.Arg0 SERVICE_LIMIT_REACHED =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "SERVICE_LIMIT_REACHED", -1);

    /**
     * Set Geometrytype for Layer '%s' to '%s'
     */
    public static final LocalizableMessageDescriptor.Arg2<Object, Object> SET_GEOMETRYTYPE_FOR_LAYER_NAME_TO_GEOTYPE =
                    new LocalizableMessageDescriptor.Arg2<Object, Object>(FrameworkMessages.class, RESOURCE, "SET_GEOMETRYTYPE_FOR_LAYER_NAME_TO_GEOTYPE", -1);

    /**
     * Set type of '%s' to Table
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> SET_TYPE_OF_NAME_TO_TABLE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "SET_TYPE_OF_NAME_TO_TABLE", -1);

    /**
     * SRS transformations supported
     */
    public static final LocalizableMessageDescriptor.Arg0 SRS_TRANSFORMATIONS_SUPPORTED_NAME =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "SRS_TRANSFORMATIONS_SUPPORTED_NAME", -1);

    /**
     * The feature of type '%s' is missing a gml:id/fid attribute. Please configure a custom id field in the layer configuration.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_FEATURES_OF_TYPE_NAME_MISS_A_GML_ID_FID_ATTRIBUTE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_FEATURES_OF_TYPE_NAME_MISS_A_GML_ID_FID_ATTRIBUTE", -1);

    /**
     * The feature of type '%s' do not respond correctly to resource id querys. To use the htmlpopup please configure a custom id field in the layer configuration.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_FEATURE_OF_TYPE_NAME_DES_NOT_SUPPORT_RESIDQUERY =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_FEATURE_OF_TYPE_NAME_DES_NOT_SUPPORT_RESIDQUERY", -1);

    /**
     * The geometry type '%s' is not supported by this service
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_GEOMETRY_TYPE_IS_NOT_SUPPORTED_BY_THIS_SERVICE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_GEOMETRY_TYPE_IS_NOT_SUPPORTED_BY_THIS_SERVICE", -1);

    /**
     * The operation '%s' is unknown.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_OPERATION_IS_UNKNOWN =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_OPERATION_IS_UNKNOWN", -1);

    /**
     * The requested inSR '%s' is not supported by this service
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_REQUESTED_INSR_IS_NOT_SUPPORTED_BY_THIS_SERVICE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_REQUESTED_INSR_IS_NOT_SUPPORTED_BY_THIS_SERVICE", -1);

    /**
     * The requested outSR '%s' is not supported by this service
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_REQUESTED_OUTSR_IS_NOT_SUPPORTED_BY_THIS_SERVICE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_REQUESTED_OUTSR_IS_NOT_SUPPORTED_BY_THIS_SERVICE", -1);

    /**
     * The spatial relation '%s' is not supported by this service
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_SPATIAL_RELATION_IS_NOT_SUPPORTED_BY_THIS_SERVICE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_SPATIAL_RELATION_IS_NOT_SUPPORTED_BY_THIS_SERVICE", -1);

    /**
     * The SRS '%s' is not supported by this service!
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> THE_SRS_NAME_IS_NOT_SUPPORTED_BY_THIS_SERVICE =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "THE_SRS_NAME_IS_NOT_SUPPORTED_BY_THIS_SERVICE", -1);

    /**
     * This service contains no layers or tables.
     */
    public static final LocalizableMessageDescriptor.Arg0 THIS_SERVICE_CONTAINS_NO_LAYERS_OR_TABLES =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "THIS_SERVICE_CONTAINS_NO_LAYERS_OR_TABLES", -1);

    /**
     * Tip: put ' ' around a character value
     */
    public static final LocalizableMessageDescriptor.Arg0 TIP_PUT_QUOTES_AROUND_A_CHARACTER_VALUE =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "TIP_PUT_QUOTES_AROUND_A_CHARACTER_VALUE", -1);

    /**
     * Transformation of LatLonBoundingBox (%d, %d, %d, %d) failed, using world extent
     */
    public static final LocalizableMessageDescriptor.Arg4<Number, Number, Number, Number> TRANSFORMATION_OF_LATLONBOUNDINGBOX_FAILED_USING_WORLD_EXTENT =
                    new LocalizableMessageDescriptor.Arg4<Number, Number, Number, Number>(FrameworkMessages.class, RESOURCE, "TRANSFORMATION_OF_LATLONBOUNDINGBOX_FAILED_USING_WORLD_EXTENT", -1);

    /**
     * Transformation of query envelope '%s' failed, aborting request.
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> TRANSFORMATION_OF_QUERY_ENVELOPE_FAILED_ABORTING_REQUEST =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "TRANSFORMATION_OF_QUERY_ENVELOPE_FAILED_ABORTING_REQUEST", -1);

    /**
     * updating WFS2GSFS service with id '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> UPDATING_WFS2GSFS_SERVICE_WITH_ID_ID =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "UPDATING_WFS2GSFS_SERVICE_WITH_ID_ID", -1);

    /**
     * Version set to '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> VERSION_SET_TO =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "VERSION_SET_TO", -1);

    /**
     * WFS does not support HTTP method POST, using GET instead.
     */
    public static final LocalizableMessageDescriptor.Arg0 WFS_DOES_NOT_SUPPORT_HTTP_METHOD_POST_USING_GET_INSTEAD =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "WFS_DOES_NOT_SUPPORT_HTTP_METHOD_POST_USING_GET_INSTEAD", -1);

    /**
     * WFS FeatureType '%s'
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> WFS_FEATURETYPE_NAME =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "WFS_FEATURETYPE_NAME", -1);

    /**
     * WFS request submitted
     */
    public static final LocalizableMessageDescriptor.Arg0 WFS_REQUEST_SUBMITTED =
                    new LocalizableMessageDescriptor.Arg0(FrameworkMessages.class, RESOURCE, "WFS_REQUEST_SUBMITTED", -1);

    /**
     * XSLT error %s
     */
    public static final LocalizableMessageDescriptor.Arg1<Object> XSLT_ERROR =
                    new LocalizableMessageDescriptor.Arg1<Object>(FrameworkMessages.class, RESOURCE, "XSLT_ERROR", -1);

}
