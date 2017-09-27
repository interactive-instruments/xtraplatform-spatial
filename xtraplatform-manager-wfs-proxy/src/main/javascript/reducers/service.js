
//selectors
export const getSelectedService = (state) => state.router.params.id
export const getSelectedFeatureType = (state) => state.router.params.ftid
export const getSelectedProperty = (state) => state.service.selectedProperty

export const getCatalog = (state) => state.entities.catalog

export const getServices = (state) => state.entities.services
export const getService = (state) => (getSelectedService(state) && state.entities.serviceConfigs) ? state.entities.serviceConfigs[getSelectedService(state)] : null //state.service.entities.services[state.service.selectedService]
export const getFeatureTypes = (state) => {
    const service = getService(state);
    let fts = [];
    if (service && service.featureTypes) {
        for (var i = 0; i < service.featureTypes.length; i++) {
            const key = service.featureTypes[i];
            //fts[key] = state.entities.featureTypes[key]
            fts.push(state.entities.featureTypes[key])
        }
        fts = fts.sort((a, b) => a.name > b.name ? 1 : -1);
    }
    return fts;
}
export const getFeatureType = (state) => {
    const service = getService(state);
    if (service && state.entities.featureTypes) {
        for (var key in state.entities.featureTypes) {
            if (state.entities.featureTypes[key].name === getSelectedFeatureType(state)) {
                return state.entities.featureTypes[key];
            }
        }
    }
    return null;
}

export const getMappingsForFeatureType = (state) => {
    const featureType = getFeatureType(state);
    let mappings = {}
    if (featureType && state.entities.mappings) {
        for (var i = 0; i < featureType.mappings.length; i++) {
            let mapping = state.entities.mappings[featureType.mappings[i]];
            let {id, index, ...rest} = mapping;
            mappings[mapping.id] = rest
        }
    }
    return mappings;
}

