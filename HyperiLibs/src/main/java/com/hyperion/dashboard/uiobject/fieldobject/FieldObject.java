package com.hyperion.dashboard.uiobject.fieldobject;

import com.hyperion.common.Constants;
import com.hyperion.common.ID;

import javafx.scene.Group;

public abstract class FieldObject {

    public ID id;
    public Constants constants;
    public Group displayGroup;
    public Group selection;

    public abstract void createDisplayGroup();
    public abstract void addDisplayGroup();
    public abstract void refreshDisplayGroup();
    public abstract void removeDisplayGroup();

}
