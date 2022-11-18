// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl.ied;

import org.junit.jupiter.api.Test;
import org.lfenergy.compas.scl2007b4.model.*;
import org.lfenergy.compas.sct.commons.dto.DTO;
import org.lfenergy.compas.sct.commons.dto.DataSetInfo;
import org.lfenergy.compas.sct.commons.dto.ExtRefSignalInfo;
import org.lfenergy.compas.sct.commons.dto.ReportControlBlock;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.ObjectReference;
import org.lfenergy.compas.sct.commons.scl.SclRootAdapter;
import org.lfenergy.compas.sct.commons.scl.com.SubNetworkAdapter;
import org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.lfenergy.compas.sct.commons.testhelpers.SclTestMarshaller.assertIsMarshallable;

class IEDAdapterTest {

    private static final String SCD_IED_U_TEST = "/ied-test-schema-conf/ied_unit_test.xml";


    @Test
    void testAmChildElementRef() throws ScdException {
        SclRootAdapter sclRootAdapter = new SclRootAdapter("hID","hVersion","hRevision");
        TIED tied = new TIED();
        tied.setName(DTO.HOLDER_IED_NAME);

        tied.setServices(new TServices());
        sclRootAdapter.getCurrentElem().getIED().add(tied);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName(DTO.HOLDER_IED_NAME);
        assertTrue(iAdapter.amChildElementRef());
        assertNotNull(iAdapter.getServices());
        assertEquals(DTO.HOLDER_IED_NAME,iAdapter.getName());

        IEDAdapter fAdapter = new IEDAdapter(sclRootAdapter);
        TIED tied1 = new TIED();
        assertThrows(IllegalArgumentException.class,
                () ->fAdapter.setCurrentElem(tied1));

        assertThrows(ScdException.class,
                () -> sclRootAdapter.getIEDAdapterByName(DTO.HOLDER_IED_NAME + "1"));

    }

    @Test
    void streamLDeviceAdapters_should_return_all_lDevices() throws Exception {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        Stream<LDeviceAdapter> result = iAdapter.streamLDeviceAdapters();
        // Then
        assertThat(result).hasSize(3);
    }

    @Test
    void findLDeviceAdapterByLdInst_should_return_LDevice() throws Exception {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        Optional<LDeviceAdapter> result = iAdapter.findLDeviceAdapterByLdInst("LD_INS1");
        // Then
        assertThat(result.map(LDeviceAdapter::getInst)).hasValue("LD_INS1");
    }

    @Test
    void findLDeviceAdapterByLdInst_when_not_found_should_return_empty_optional() throws Exception {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        Optional<LDeviceAdapter> result = iAdapter.findLDeviceAdapterByLdInst("NOT_EXISTING");
        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getLDeviceAdapterByLdInst_should_return_LDevice() throws Exception {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When
        LDeviceAdapter result = iAdapter.getLDeviceAdapterByLdInst("LD_INS1");
        // Then
        assertThat(result.getInst()).isEqualTo("LD_INS1");
    }

    @Test
    void getLDeviceAdapterByLdInst_when_not_found_should_throw_exception() throws Exception {
        // Given
        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = sclRootAdapter.getIEDAdapterByName("IED_NAME");
        // When & Then
        assertThatThrownBy(() -> iAdapter.getLDeviceAdapterByLdInst("NOT_EXISTING"))
            .isInstanceOf(ScdException.class)
            .hasMessage("LDevice.inst 'NOT_EXISTING' not found in IED 'IED_NAME'");
    }

    @Test
    void testUpdateLDeviceNodesType() throws Exception {

        SCL scd = SclTestMarshaller.getSCLFromFile(SCD_IED_U_TEST);
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow( () -> sclRootAdapter.getIEDAdapterByName(DTO.HOLDER_IED_NAME));

        assertTrue(iAdapter.streamLDeviceAdapters().count() >= 2);
        Map<String,String> pairOldNewId = new HashMap<>();
        pairOldNewId.put("LNO1", DTO.HOLDER_IED_NAME + "_LNO1");
        pairOldNewId.put("LNO2", DTO.HOLDER_IED_NAME + "_LNO2");
        assertDoesNotThrow( () ->iAdapter.updateLDeviceNodesType(pairOldNewId));

        LDeviceAdapter lDeviceAdapter = iAdapter.streamLDeviceAdapters().findFirst().get();
        assertEquals(DTO.HOLDER_IED_NAME + "_LNO1",lDeviceAdapter.getLN0Adapter().getLnType());
    }

    @Test
    void testGetExtRefBinders() throws Exception {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));
        ExtRefSignalInfo signalInfo = DTO.createExtRefSignalInfo();
        signalInfo.setPDO("Do.sdo1");
        signalInfo.setPDA("da.bda1.bda2.bda3");
        assertDoesNotThrow(() ->iAdapter.getExtRefBinders(signalInfo));

        signalInfo.setPDO("Do.sdo1.errorSdo");
        assertThrows(ScdException.class, () ->iAdapter.getExtRefBinders(signalInfo));
    }

    @Test
    void TestIsSettingConfig() throws Exception {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        assertTrue(iAdapter.isSettingConfig("LD_INS1"));

        assertThrows(IllegalArgumentException.class,() -> iAdapter.isSettingConfig("UnknownLD"));
    }

    @Test
    void testMatches() throws Exception {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        ObjectReference objectReference = new ObjectReference("IED_NAMELD_INS3/LLN0.Do.da2");
        objectReference.init();
        assertTrue(iAdapter.matches(objectReference));

        objectReference = new ObjectReference("IED_NAMELD_INS2/ANCR1.dataSet");
        objectReference.init();
        assertTrue(iAdapter.matches(objectReference));
    }


    @Test
    void testCreateDataSet() throws Exception {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        assertThrows(ScdException.class, () -> iAdapter.createDataSet(new DataSetInfo()));

        TServices tServices = new TServices();
        iAdapter.getCurrentElem().setServices(tServices);
        assertFalse(iAdapter.hasDataSetCreationCapability());

        TLogSettings tLogSettings = new TLogSettings();
        tServices.setLogSettings(tLogSettings);
        tLogSettings.setDatSet(TServiceSettingsEnum.CONF);
        assertTrue(iAdapter.hasDataSetCreationCapability());

        TDataSet tDataSet = new TDataSet();
        tDataSet.setName("dataset");
        TFCDA tfcda = new TFCDA();
        tfcda.setFc(TFCEnum.ST);
        tDataSet.getFCDA().add(tfcda);
        DataSetInfo dataSetInfo = DataSetInfo.from(tDataSet);
        dataSetInfo.setHolderIEDName("IED_NAME");
        assertThrows(ScdException.class, () -> iAdapter.createDataSet(dataSetInfo));
        dataSetInfo.setHolderLDInst("LD_INS2");
        dataSetInfo.setHolderLnClass(TLLN0Enum.LLN_0.value());
        assertDoesNotThrow(() -> iAdapter.createDataSet(dataSetInfo));

        assertIsMarshallable(scd);
    }

    @Test
    void testHasDataSetCreationCapability() throws Exception {
        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        assertFalse(iAdapter.hasDataSetCreationCapability());

        TServices tServices = new TServices();
        iAdapter.getCurrentElem().setServices(tServices);
        assertFalse(iAdapter.hasDataSetCreationCapability());

        TLogSettings tLogSettings = new TLogSettings();
        tServices.setLogSettings(tLogSettings);
        tLogSettings.setDatSet(TServiceSettingsEnum.CONF);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        tLogSettings.setDatSet(TServiceSettingsEnum.DYN);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        tLogSettings.setDatSet(TServiceSettingsEnum.FIX);
        assertFalse(iAdapter.hasDataSetCreationCapability());

        TGSESettings tgseSettings = new TGSESettings();
        tServices.setGSESettings(tgseSettings);

        tgseSettings.setDatSet(TServiceSettingsEnum.CONF);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        tgseSettings.setDatSet(TServiceSettingsEnum.DYN);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        tgseSettings.setDatSet(TServiceSettingsEnum.FIX);
        assertFalse(iAdapter.hasDataSetCreationCapability());

        TReportSettings reportSettings = new TReportSettings();
        tServices.setReportSettings(reportSettings);

        reportSettings.setDatSet(TServiceSettingsEnum.CONF);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        reportSettings.setDatSet(TServiceSettingsEnum.DYN);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        reportSettings.setDatSet(TServiceSettingsEnum.FIX);
        assertFalse(iAdapter.hasDataSetCreationCapability());


        TSMVSettings tsmvSettings = new TSMVSettings();
        tServices.setSMVSettings(tsmvSettings);

        tsmvSettings.setDatSet(TServiceSettingsEnum.CONF);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        tsmvSettings.setDatSet(TServiceSettingsEnum.DYN);
        assertTrue(iAdapter.hasDataSetCreationCapability());
        tsmvSettings.setDatSet(TServiceSettingsEnum.FIX);
        assertFalse(iAdapter.hasDataSetCreationCapability());

    }

    @Test
    void createControlBlock() throws Exception {

        SCL scd = SclTestMarshaller.getSCLFromFile("/ied-test-schema-conf/ied_unit_test.xml");
        SclRootAdapter sclRootAdapter = new SclRootAdapter(scd);
        IEDAdapter iAdapter = assertDoesNotThrow(() -> sclRootAdapter.getIEDAdapterByName("IED_NAME"));

        ReportControlBlock controlBlock = new ReportControlBlock();
        controlBlock.setName("rpt");
        controlBlock.setConfRev(2L);

        controlBlock.setHolderIEDName("IED_NAME");
        controlBlock.setHolderIEDName("IED_NAME");
        controlBlock.setHolderIEDName("IED_NAME");
        assertIsMarshallable(scd);
    }

    @Test
    void addPrivate() {
        SclRootAdapter sclRootAdapter = Mockito.mock(SclRootAdapter.class);
        SCL scl = Mockito.mock(SCL.class);
        Mockito.when(sclRootAdapter.getCurrentElem()).thenReturn(scl);
        TIED tied = new TIED();
        Mockito.when(scl.getIED()).thenReturn(List.of(tied));
        IEDAdapter iAdapter = new IEDAdapter(sclRootAdapter, tied);
        TPrivate tPrivate = new TPrivate();
        tPrivate.setType("Private Type");
        tPrivate.setSource("Private Source");
        assertTrue(iAdapter.getCurrentElem().getPrivate().isEmpty());
        iAdapter.addPrivate(tPrivate);
        assertEquals(1, iAdapter.getCurrentElem().getPrivate().size());
    }

    @Test
    void elementXPath() {
        // Given
        SclRootAdapter sclRootAdapter = Mockito.mock(SclRootAdapter.class);
        SCL scl = Mockito.mock(SCL.class);
        Mockito.when(sclRootAdapter.getCurrentElem()).thenReturn(scl);
        TIED tied = new TIED();
        tied.setName("iedName");
        Mockito.when(scl.getIED()).thenReturn(List.of(tied));
        IEDAdapter iAdapter = new IEDAdapter(sclRootAdapter, tied);
        // When
        String result = iAdapter.elementXPath();
        // Then
        assertThat(result).isEqualTo("IED[@name=\"iedName\"]");
    }

}
