import React, { Component } from 'react';
import PropTypes from 'prop-types';
import ui from 'redux-ui';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Heading from 'grommet/components/Heading';
import Form from 'grommet/components/Form';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import TextInput from 'grommet/components/TextInput';

import TextInputUi from 'xtraplatform-manager/components/common/TextInputUi';

@ui({
    state: {
        displayName: (props) => props.featureType.displayName || ''
    }
})

export default class FeatureTypeEditGeneral extends Component {

    _save = () => {
        const {ui, onChange, featureType} = this.props;

        onChange(ui);
    }

    render() {
        const {featureType, ui, updateUI} = this.props;

        return (
            featureType && <Section pad={ { vertical: 'medium' } } full="horizontal">
                               <Box pad={ { horizontal: 'medium' } } separator="bottom">
                                   <Heading tag="h2">
                                       General
                                   </Heading>
                               </Box>
                               <Form compact={ false } pad={ { horizontal: 'medium', vertical: 'small' } }>
                                   <FormFields>
                                       <fieldset>
                                           <FormField label="Id">
                                               <TextInput name="name" value={ featureType.name } disabled={ true } />
                                           </FormField>
                                           <FormField label="Display name">
                                               <TextInputUi name="displayName"
                                                   value={ ui.displayName }
                                                   onChange={ updateUI }
                                                   onDebounce={ this._save } />
                                           </FormField>
                                       </fieldset>
                                   </FormFields>
                               </Form>
                           </Section>
        );
    }
}

FeatureTypeEditGeneral.propTypes = {
    onChange: PropTypes.func.isRequired
};

FeatureTypeEditGeneral.defaultProps = {
};