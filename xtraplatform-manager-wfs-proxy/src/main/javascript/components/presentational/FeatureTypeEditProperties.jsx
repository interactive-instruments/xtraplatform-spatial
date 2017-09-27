import React, { Component, PropTypes } from 'react';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Heading from 'grommet/components/Heading';
import Button from 'grommet/components/Button';
import Title from 'grommet/components/Title';
import Paragraph from 'grommet/components/Paragraph';

import GrommetTreeList from 'xtraplatform-manager/components/common/GrommetTreeList';

class FeatureTypeEditProperties extends Component {

    render() {
        const {tree, selected, expanded, mappingStatus, onExpand, onSelect, onActivate} = this.props;


        const showButton = mappingStatus && !mappingStatus.enabled;
        const showLoading = mappingStatus && mappingStatus.enabled && mappingStatus.loading;
        const showError = mappingStatus && mappingStatus.enabled && !mappingStatus.loading && !mappingStatus.supported;
        const showMapping = mappingStatus && mappingStatus.enabled && mappingStatus.supported && !mappingStatus.loading;

        return (
            <Section pad={ { vertical: 'none' } } full="horizontal">
                <Box pad={ { horizontal: 'medium' } } separator={ !showMapping && 'bottom' }>
                    <Heading tag="h2">
                        Mapping
                    </Heading>
                </Box>
                { showButton && <Box pad={ { horizontal: 'medium', vertical: 'small' } }>
                                    <Button label='Enable' secondary={ true } onClick={ onActivate } />
                                </Box> }
                { showError && <Box pad={ { horizontal: 'medium', vertical: 'small' } }>
                                   <Title>
                                       There is an issue with the service:
                                   </Title>
                                   <Paragraph>
                                       { mappingStatus.errorMessage }
                                   </Paragraph>
                                   { mappingStatus.errorMessageDetails
                                     && mappingStatus.errorMessageDetails.map(detail => <Paragraph margin='none'>
                                                                                            { detail }
                                                                                        </Paragraph>) }
                               </Box> }
                { showLoading && <Box pad={ { horizontal: 'medium', vertical: 'small' } }>
                                     <Button label='Loading...' secondary={ true } />
                                 </Box> }
                { showMapping && <GrommetTreeList tree={ tree }
                                     expanded={ expanded }
                                     selected={ selected }
                                     onExpand={ onExpand }
                                     onSelect={ onSelect } /> }
            </Section>
        );
    }
}

export default FeatureTypeEditProperties;
