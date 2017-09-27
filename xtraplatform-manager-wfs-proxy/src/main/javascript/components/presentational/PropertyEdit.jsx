import React, { Component, PropTypes } from 'react';

import Section from 'grommet/components/Section';
import Article from 'grommet/components/Article';
import Sidebar from 'grommet/components/Sidebar';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Box from 'grommet/components/Box';
import Form from 'grommet/components/Form';

import MappingEdit from './MappingEdit';

class PropertyEdit extends Component {

    _onMappingChange = (change) => {
        const {qn, onChange} = this.props;

        onChange({
            mappings: {
                mappings: {
                    [qn]: change
                }
            }
        });
    }

    render() {
        const {title, mappings, isFeatureType} = this.props;

        let properties = [];

        return (
            <Sidebar size="medium" colorIndex="light-2">
                <div>
                    <Header pad={ { horizontal: "small", vertical: "medium" } }
                        justify="between"
                        size="large"
                        colorIndex="light-2">
                        <Heading tag="h2"
                            margin="none"
                            strong={ true }
                            truncate={ true }>
                            { title }
                        </Heading>
                    </Header>
                    <Article align="center" pad={ { horizontal: 'medium' } } primary={ true }>
                        <Form compact={ true }>
                            { Object.keys(mappings).map(
                                  (mimeType) => <Section key={ mimeType } pad={ { vertical: 'medium' } } full="horizontal">
                                                    <MappingEdit mimeType={ mimeType }
                                                        mapping={ mappings[mimeType][0] }
                                                        isFeatureType={ isFeatureType }
                                                        onChange={ this._onMappingChange } />
                                                </Section>
                              ) }
                            <Box pad={ { vertical: 'large' } } />
                        </Form>
                    </Article>
                </div>
            </Sidebar>
        );
    }
}

export default PropertyEdit;