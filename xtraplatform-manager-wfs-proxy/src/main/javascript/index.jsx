import merge from 'deepmerge'

import { app, render } from 'xtraplatform-manager/module'
import mdl from './module'

//import 'xtraplatform-manager/scss/default'
import './scss/ldproxy'

const cfg = merge(app, mdl)

console.log('HELLO WFS', cfg)

render(cfg);

// Hot Module Replacement API
if (module && module.hot) {
    module.hot.accept('./module.js', () => {
        render(cfg);
    });
}