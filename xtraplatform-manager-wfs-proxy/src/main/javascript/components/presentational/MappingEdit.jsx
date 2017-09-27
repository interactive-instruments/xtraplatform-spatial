import React, { Component, PropTypes } from 'react';

import Article from 'grommet/components/Article';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Form from 'grommet/components/Form';
import Footer from 'grommet/components/Footer';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import TextInput from 'grommet/components/TextInput';
import Select from 'grommet/components/Select';
import CheckBox from 'grommet/components/CheckBox';
import Label from 'grommet/components/Label';
import Collapsible from 'grommet/components/Collapsible';

class MappingEdit extends Component {

    constructor(props) {
        super(props);

        this.state = {
            enabled: props.mapping.enabled || false,
            name: props.mapping.name || (props.isFeatureType && props.mimeType === 'text/html' ? '{{id}}' : ''),
            type: props.mapping.type || '',
            showInCollection: props.mapping.showInCollection || false,
            itemType: props.mapping.itemType || '',
            itemProp: props.mapping.itemProp || '',
            mappingType: props.mapping.mappingType || ''
        }

        this._timers = {}
    }

    componentWillReceiveProps(nextProps) {

        if (this.props.mapping != nextProps.mapping) {

            this.setState({
                enabled: nextProps.mapping.enabled || false,
                name: nextProps.mapping.name || (nextProps.isFeatureType && nextProps.mimeType === 'text/html' ? '{{id}}' : ''),
                type: nextProps.mapping.type || '',
                showInCollection: nextProps.mapping.showInCollection || false,
                itemType: nextProps.mapping.itemType || '',
                itemProp: nextProps.mapping.itemProp || '',
                mappingType: nextProps.mapping.mappingType || ''
            })
        }
    }

    _handleInputChange = (event) => {
        const {mapping, mimeType, onChange} = this.props;

        const target = event.target;
        const value = target.type === 'checkbox' ? target.checked : (event.option ? event.option.value : target.value);
        const name = target.name;

        clearTimeout(this._timers[mimeType + name]);
        this._timers[mimeType + name] = setTimeout(() => {
            onChange({
                [mimeType]: [{
                    ...this.state,
                    [name]: value
                }]
            });
        }, 1000);

        this.setState({
            [name]: value
        });


    }

    render() {
        let {mapping, mimeType, isFeatureType} = this.props;
        let {enabled, name, type, showInCollection, itemProp, itemType} = this.state;

        const header = <Heading tag="h4"
                           strong={ true }
                           margin="none"
                           truncate={ true }
                           uppercase={ false }>
                           { mimeType }
                       </Heading>

        return (
            <FormFields>
                <fieldset>
                    <Header size="small" justify="between" pad="none">
                        { isFeatureType ? header : <CheckBox name="enabled"
                                                       checked={ enabled }
                                                       toggle={ true }
                                                       reverse={ false }
                                                       label={ header }
                                                       onChange={ this._handleInputChange } /> }
                    </Header>
                    <Collapsible active={ enabled }>
                        { (!isFeatureType || mimeType === 'text/html') && <FormField label="Name">
                                                                              <TextInput name="name" value={ name } onDOMChange={ this._handleInputChange } />
                                                                          </FormField> }
                        { !isFeatureType && <FormField label="LD Type">
                                                <TextInput name="itemProp" value={ itemProp } onDOMChange={ this._handleInputChange } />
                                            </FormField> }
                        { isFeatureType && <FormField label="LD Type">
                                               <TextInput name="itemType" value={ itemType } onDOMChange={ this._handleInputChange } />
                                           </FormField> }
                        { !isFeatureType && <FormField label="Type">
                                                { (type === 'ID' || type === 'GEOMETRY') ?
                                                  <Select name="type"
                                                      value={ type }
                                                      options={ [type] }
                                                      onChange={ () => {
                                                                 } } />
                                                  : <Select name="type"
                                                        value={ { value: type, label: type === 'STRING' ? 'String' : 'Number' } }
                                                        options={ [{ value: 'STRING', label: 'String' }, { value: 'NUMBER', label: 'Number' }] }
                                                        onChange={ this._handleInputChange } /> }
                                            </FormField> }
                        { !isFeatureType && <FormField label="Show in collection">
                                                <CheckBox name="showInCollection"
                                                    checked={ showInCollection }
                                                    toggle={ true }
                                                    reverse={ false }
                                                    onChange={ this._handleInputChange } />
                                            </FormField> }
                    </Collapsible>
                </fieldset>
            </FormFields>
        );
    }
}

export default MappingEdit;