import { normalizeServices, normalizeServiceConfigs } from 'xtraplatform-manager/apis/ServiceNormalizer'
import ServiceApi from 'xtraplatform-manager/apis/ServiceApi'


const ServiceApiWfsProxy = {
    getServiceConfigQuery: function(id) {
        return {
            url: `/rest/admin/services/${id}/config/`,
            transform: (serviceConfig) => normalizeServiceConfigs([serviceConfig]).entities,
            update: {
                serviceConfigs: (prev, next) => next,
                featureTypes: (prev, next) => next,
                mappings: (prev, next) => next
            },
            force: true
        }
    },

    updateFeatureTypeQuery: function(id, ftid, ftqn, change) {
        console.log({
            id: id,
            featureTypes: {
                [ftqn]: change
            }
        });
        return {
            url: `/rest/admin/services/${id}/`,
            body: JSON.stringify({
                id: id,
                featureTypes: {
                    [ftqn]: change
                }
            }),
            options: {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            },
            optimisticUpdate: {
                featureTypes: (prev) => Object.assign({}, prev, {
                    [ftid]: {
                        ...prev[ftid],
                        ...change
                    }
                })
            }
        }
    },

    parseCatalogQuery: function(url) {
        return {
            url: `/rest/catalog/`,
            transform: (catalog) => ({
                catalog: catalog
            }),
            body: {
                url: url
            },
            options: {
                method: 'GET'
            },
            update: {
                catalog: (prev, next) => next
            },
            force: true
        }
    }
}


export default {
    ...ServiceApi,
    ...ServiceApiWfsProxy
};