
import ServiceAddWfsProxy from './components/container/ServiceAddWfsProxy'
import ServiceShowWfsProxy from './components/container/ServiceShowWfsProxy'
import FeatureTypeShow from './components/container/FeatureTypeShow'
import ServiceAddCatalog from './components/container/ServiceAddCatalog'

export default {
    applicationName: 'ldproxy',
    serviceTypes: ['ldproxy'],
    routes: {
        path: '/',
        routes: [
            {
                path: '/services',
                routes: [
                    {
                        path: '/addcatalog',
                        component: ServiceAddCatalog
                    },
                    {},
                    {
                        path: '/:id/:ftid',
                        component: FeatureTypeShow
                    },
                    {},
                    {}
                ]
            }
        ]
    },
    typedComponents: {
        ServiceAdd: {
            ldproxy: ServiceAddWfsProxy
        },
        ServiceShow: {
            ldproxy: ServiceShowWfsProxy
        }
    },
    typeLabels: {
        ldproxy: 'ldproxy'
    },
    serviceMenu: [{
        label: 'Add from catalog',
        path: '/services/addcatalog',
        description: 'Add services from catalog'
    }]

};

