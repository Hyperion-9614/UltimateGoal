package com.hyperion.dashboard.uiobject;

import com.hyperion.common.Constants;

import javafx.scene.Group;

public abstract class FieldObject {

    public String id;
    public Constants constants;
    public Group displayGroup;

    public abstract void createDisplayGroup();
    public abstract void addDisplayGroup();
    public abstract void refreshDisplayGroup();
    public abstract void removeDisplayGroup();

    public abstract void select();
    public abstract void deselect();

}
