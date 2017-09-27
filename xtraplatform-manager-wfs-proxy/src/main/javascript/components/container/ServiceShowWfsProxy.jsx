import React, { Component } from 'react';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'

import Heading from 'grommet/components/Heading';
import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import List from 'grommet/components/List';
import ListItem from 'grommet/components/ListItem';
import ListPlaceholder from 'grommet-addons/components/ListPlaceholder';

import ServiceShow from 'xtraplatform-manager/components/container/ServiceShow'

import { push } from 'redux-little-router'


// TODO:
import { getService, getFeatureTypes } from '../../reducers/service'


@connect(
    (state, props) => {
        console.log('CONNECT', props.urlParams.id, state.entities.serviceConfigs)
        return {
            service: getService(state, props.urlParams.id),
            featureTypes: getFeatureTypes(state, props.urlParams.id)
        }
    })

export default class ServiceShowWfsProxy extends Component {

    _renderFeatureTypes() {
        const {featureTypes} = this.props;

        let fts = []

        if (featureTypes)
            fts = featureTypes.map((ft, i) => <ListItem key={ ft.id } separator={ i === 0 ? 'horizontal' : 'bottom' } onClick={ this._select(ft.name) }>
                                                  { ft.displayName }
                                              </ListItem>)
        return (
            <Section pad={ { vertical: 'medium' } } full="horizontal">
                <Box pad={ { horizontal: 'medium' } }>
                    <Heading tag="h2">
                        Feature Types
                    </Heading>
                </Box>
                <List>
                    { fts.length <= 0 ?
                      <ListPlaceholder /> :
                      fts }
                </List>
            </Section>
        );
    }

    // TODO
    _select = (fid) => {
        const {service, dispatch} = this.props;
        return () => {
            var sid = service.id;
            console.log('selected: ', sid, fid);
            // TODO: save in store and push via action, see ferret
            //this.props.dispatch(actions.selectFeatureType(fid));
            dispatch(push('/services/' + sid + '/' + fid));
        };

    }

    render() {
        console.log('CONNECTED', this.props)
        let fts;
        fts = this._renderFeatureTypes();

        return (
            <ServiceShow {...this.props}>
                { fts }
            </ServiceShow>
        );
    }
}
