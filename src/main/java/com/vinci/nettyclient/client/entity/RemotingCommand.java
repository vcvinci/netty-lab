package com.vinci.nettyclient.client.entity;

public class RemotingCommand {


    private int id;
    private String remark;
    private int opaque;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setOpaque(int opaque) {
        this.opaque = opaque;
    }

    @Override
    public String toString() {
        return "RemotingCommand{" +
                "id=" + id +
                ", remark='" + remark + '\'' +
                ", opaque=" + opaque +
                '}';
    }

    public int getOpaque() {
        return opaque;
    }

}
