// SPDX-FileCopyrightText: 2021 RTE FRANCE
//
// SPDX-License-Identifier: Apache-2.0

package org.lfenergy.compas.sct.commons.scl.sstation;

import org.lfenergy.compas.scl2007b4.model.TSubstation;
import org.lfenergy.compas.sct.commons.exception.ScdException;
import org.lfenergy.compas.sct.commons.scl.SclElementAdapter;
import org.lfenergy.compas.sct.commons.scl.SclRootAdapter;
import org.lfenergy.compas.sct.commons.util.Utils;

import java.util.Optional;
import java.util.stream.Stream;

public class SubstationAdapter extends SclElementAdapter<SclRootAdapter, TSubstation> {

    public SubstationAdapter(SclRootAdapter parentAdapter) {
        super(parentAdapter);
    }

    public SubstationAdapter(SclRootAdapter parentAdapter, TSubstation currentElem) {
        super(parentAdapter, currentElem);
    }

    public SubstationAdapter(SclRootAdapter parentAdapter, String ssName) throws ScdException {
        super(parentAdapter);
        TSubstation tSubstation = parentAdapter.getCurrentElem().getSubstation()
                .stream()
                .filter(subst -> subst.getName().equals(ssName))
                .findFirst()
                .orElseThrow(() -> new ScdException("Unknown Substation name :" + ssName));
        setCurrentElem(tSubstation);
    }

    @Override
    protected boolean amChildElementRef() {
        return parentAdapter.getCurrentElem().getSubstation().contains(currentElem);
    }

    @Override
    protected String elementXPath() {
        return String.format("Substation[%s]", Utils.xpathAttributeFilter("name", currentElem.isSetName() ? currentElem.getName() : null));
    }

    public Optional<VoltageLevelAdapter> getVoltageLevelAdapter(String vLevelName) {
        return currentElem.getVoltageLevel()
                .stream()
                .filter(tVoltageLevel -> tVoltageLevel.getName().equals(vLevelName))
                .map(tVoltageLevel -> new VoltageLevelAdapter(this, tVoltageLevel))
                .findFirst();
    }

    public Stream<VoltageLevelAdapter> streamVoltageLevelAdapters() {
        if (!currentElem.isSetVoltageLevel()){
            return Stream.empty();
        }
        return currentElem.getVoltageLevel().stream().map(tVoltageLevel -> new VoltageLevelAdapter(this, tVoltageLevel));
    }

}