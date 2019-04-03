package com.github.pertempto.backyardsportstracker.data;

import java.util.Objects;

public class BaseDataObject {
    public long id;
    public boolean deleted;

    public BaseDataObject() {
        this.id = 0;
        this.deleted = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseDataObject that = (BaseDataObject) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
