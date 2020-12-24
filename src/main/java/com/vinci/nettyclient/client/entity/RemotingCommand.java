package com.vinci.nettyclient.client.entity;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RemotingCommand {

    private static AtomicInteger requestId = new AtomicInteger(0);

    private int id;
    private String name;
    private String remark;
    private int opaque = requestId.getAndIncrement();

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RemotingCommand that = (RemotingCommand) o;
        return id == that.id &&
                Objects.equals(name, that.name) &&
                Objects.equals(remark, that.remark);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, remark);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public int getOpaque() {
        return opaque;
    }
}
