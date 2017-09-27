import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { push } from 'redux-little-router'
import { mutateAsync, requestAsync, getQueryKey, cancelQuery } from 'redux-query';
import ui from 'redux-ui';

import Section from 'grommet/components/Section';
import Box from 'grommet/components/Box';
import Header from 'grommet/components/Header';
import Heading from 'grommet/components/Heading';
import Footer from 'grommet/components/Footer';
import Button from 'grommet/components/Button';
import Form from 'grommet/components/Form';
import FormFields from 'grommet/components/FormFields';
import FormField from 'grommet/components/FormField';
import LinkPreviousIcon from 'grommet/components/icons/base/LinkPrevious';
import AnnotatedMeter from 'grommet-addons/components/AnnotatedMeter';
import Table from 'grommet/components/Table';
import TableRow from 'grommet/components/TableRow';
import TableHeader from 'grommet/components/TableHeader';
import Paragraph from 'grommet/components/Paragraph';

import ServiceApi from '../../apis/ServiceApiWfsProxy'
import { getCatalog } from '../../reducers/service'
import TextInputUi from 'xtraplatform-manager/components/common/TextInputUi';
import Anchor from 'xtraplatform-manager/components/common/AnchorLittleRouter';



@ui({
    key: 'catalog',
    persist: true,
    state: {
        url: '',
        loading: false,
        loaded: false,
        error: {},
        succeeded: 0,
        failed: 0,
        waiting: 0,
        loading2: false,
        loaded2: false,
        showErrors: false,
        errors: []
    }
})

@connect(
    (state, props) => {
        return {
            catalog: getCatalog(state)
        }
    },
    (dispatch, props) => {
        return {
            parseCatalog: (url) => {
                const query = ServiceApi.parseCatalogQuery(url);
                dispatch(mutateAsync(query))
                    .then((result) => {
                        if (result.status === 200) {
                            props.updateUI({
                                loading: false,
                                loaded: true,
                                waiting: result.body.length
                            })
                        } else {
                            props.updateUI({
                                loading: false,
                                loaded: false,
                                error: result.body && result.body.error || {}
                            })
                        }
                    })
                return getQueryKey(query);
            },
            dispatch
        }
    })


export default class ServiceAddCatalog extends Component {

    _parseCatalog = (event) => {
        event.preventDefault();
        const {ui, updateUI, parseCatalog} = this.props;
        updateUI({
            loading: true
        })
        this.query = parseCatalog(ui.url);
    }

    _cancelCatalog = (event) => {
        event.preventDefault();
        const {updateUI, dispatch} = this.props;
        updateUI({
            loading: false,
            loaded: false
        })
        if (this.query) {
            dispatch(cancelQuery(this.query));
        }
    }

    _cancelServices = (event) => {
        event.preventDefault();
        const {updateUI, dispatch} = this.props;
        this._cancel = true;
        updateUI({
            loading2: false,
            loaded2: true
        })
        if (this.query) {
            console.log('CANCEL QUERY', this.query)
            dispatch(cancelQuery(this.query));
        }
    }

    _addServices = () => {
        const {ui, updateUI, catalog, dispatch} = this.props;

        var req;
        //var baseId = ui.url.replace("http://", "").replace(/\./g, "_").replace(/\//g, "_")
        var baseId = ui.url.substring(ui.url.indexOf('/') + 2, ui.url.indexOf('?') > -1 ? ui.url.indexOf('?') : ui.url.length);
        if (baseId.length > 30)
            baseId = baseId.substring(0, baseId.lastIndexOf('/', 30));
        baseId = baseId.replace(/\./g, "_").replace(/\//g, "_").replace(/-/g, "_");
        var counter = 1;

        updateUI('loading2', true)

        let service2
        catalog.forEach(url => {

            const service = {
                id: baseId + '_' + counter++,
                type: 'ldproxy',
                disableMapping: true,
                url: url
            }
            const lastService = service2;
            var query = ServiceApi.addServiceQuery(service);
            this.query = getQueryKey(query);

            if (!req) {
                req = dispatch(mutateAsync(query))
            } else {
                req = req.then(result => {
                    this._updateCatalogMeter(result);
                    if (result.status === 200)
                        setTimeout(() => dispatch(requestAsync(ServiceApi.getServiceQuery(lastService.id))), 1000);
                    if (!this._cancel) {
                        return dispatch(mutateAsync(query))
                    }
                })
            }

            service2 = service;

        })

        if (req) {
            req = req.then(result => {
                this._updateCatalogMeter(result);
                if (result.status === 200)
                    setTimeout(() => dispatch(requestAsync(ServiceApi.getServiceQuery(service2.id))), 1000);

                updateUI({
                    loading2: false,
                    loaded2: true
                })
            })
        }
    }

    _updateCatalogMeter = (result) => {
        const {updateUI} = this.props;
        if (result.status === 200) {
            updateUI('succeeded', val => val + 1)
            updateUI('waiting', val => val - 1)
        } else {
            updateUI('failed', val => val + 1)
            updateUI('waiting', val => val - 1)
            updateUI('errors', val => {
                val.push(result.body && result.body.error || {});
                return val;
            })
        }
    }

    _done = (event) => {
        const {resetUI, updateUI, dispatch} = this.props;
        resetUI();
        // TODO
        updateUI({
            errors: []
        })
        dispatch(push('/services'));
    }

    render() {
        const {ui, updateUI, catalog} = this.props;

        return (
            <div>
                <Header pad={ { horizontal: "small", vertical: "medium" } }
                    justify="between"
                    size="large"
                    colorIndex="light-2">
                    <Box direction="row"
                        align="center"
                        pad={ { between: 'small' } }
                        responsive={ false }>
                        <Anchor icon={ <LinkPreviousIcon /> } path={ '/services' } a11yTitle="Return" />
                        <Heading tag="h1" margin="none">
                            <strong>Add Services from Catalog</strong>
                        </Heading>
                    </Box>
                    { /*sidebarControl*/ }
                </Header>
                <Form compact={ false } plain={ true } pad={ { horizontal: 'large', vertical: 'medium' } }>
                    { <FormFields>
                          <fieldset>
                              <FormField label="CSW URL" style={ { width: '100%' } }>
                                  <TextInputUi name="url"
                                      autoFocus
                                      value={ ui.url }
                                      disabled={ ui.loading || ui.loaded }
                                      onChange={ updateUI } />
                              </FormField>
                          </fieldset>
                      </FormFields> }
                    { !ui.loading && ui.loaded && <AnnotatedMeter type='circle'
                                                      max={ catalog.length }
                                                      series={ [{ "label": "To be added", "value": ui.waiting, "colorIndex": "unknown" }, { "label": "Succeeded", "value": ui.succeeded, "colorIndex": "ok" }, { "label": "Failed", "value": ui.failed, "colorIndex": "critical" }] }
                                                      legend={ true } /> }
                    <Footer pad={ { "vertical": "medium" } }>
                        { this._renderButton() }
                    </Footer>
                    { ui.showErrors && ui.errors.length > 0 && <Table>
                                                                   <TableHeader labels={ ['Error', 'Message'] } />
                                                                   <tbody>
                                                                       { ui.errors.map(error => <TableRow key={ ui.errors.indexOf(error) }>
                                                                                                    <td>
                                                                                                        { error.message }
                                                                                                    </td>
                                                                                                    <td className='secondary'>
                                                                                                        { error.details.map(detail => <Paragraph margin='none'>
                                                                                                                                          { detail }
                                                                                                                                      </Paragraph>) }
                                                                                                    </td>
                                                                                                </TableRow>) }
                                                                   </tbody>
                                                               </Table> }
                </Form>
            </div>
        );
    }

    _renderButton = (result) => {
        const {ui, catalog, updateUI} = this.props;

        if (!ui.loading && !ui.loaded) {
            return <Button label='Find Services' primary={ true } onClick={ ui.url.length < 11 ? null : this._parseCatalog } />;
        } else if (ui.loading && !ui.loaded) {
            return <Box direction='row' pad={ { between: 'medium' } }>
                       <Button label='Loading...' primary={ true } />
                       <Button label={ `Cancel` } critical={ true } onClick={ this._cancelCatalog } />
                   </Box>;
        } else if (!ui.loading && ui.loaded && !ui.loading2 && !ui.loaded2) {
            return <Box direction='row' pad={ { between: 'medium' } }>
                       <Button label={ `Add ${catalog.length} Services` } primary={ true } onClick={ this._addServices } />
                       <Button label={ `Cancel` } critical={ true } onClick={ this._done } />
                   </Box>;
        } else if (!ui.loading && ui.loaded && ui.loading2 && !ui.loaded2) {
            return <Box direction='row' pad={ { between: 'medium' } }>
                       <Button label={ `Adding Services...` } primary={ true } />
                       <Button label={ `Cancel` } critical={ true } onClick={ this._cancelServices } />
                   </Box>;
        } else if (!ui.loading && ui.loaded && !ui.loading2 && ui.loaded2) {
            if (ui.errors.length > 0) {
                return <Box direction='row' pad={ { between: 'medium' } }>
                           <Button label={ `Done` } primary={ true } onClick={ this._done } />
                           <Button label={ ui.showErrors ? `Hide Errors` : `Show Errors` } critical={ true } onClick={ e => updateUI('showErrors', val => !val) } />
                       </Box>;
            }
            return <Button label={ `Done` } primary={ true } onClick={ this._done } />;
        }
    }


}
