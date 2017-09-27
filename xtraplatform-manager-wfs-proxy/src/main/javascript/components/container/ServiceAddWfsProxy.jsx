import React, { Component } from 'react';
import ui from 'redux-ui';

import FormField from 'grommet/components/FormField';
import TextInputUi from 'xtraplatform-manager/components/common/TextInputUi';
import ServiceAdd from 'xtraplatform-manager/components/container/ServiceAdd'


@ui({
    state: {
        url: '',
        type: 'ldproxy'
    }
})
export default class ServiceAddWfsProxy extends Component {

    render() {
        const {ui, updateUI, ...rest} = this.props;

        return (
            <ServiceAdd {...rest}>
                <FormField label="WFS URL" style={ { width: '100%' } }>
                    <TextInputUi name="url" value={ ui.url } onChange={ updateUI } />
                </FormField>
            </ServiceAdd>
        );
    }
}
