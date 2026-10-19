package com.hostelmanagement.model;

public class Block {

    private final long blockId;
    private final long hostelId;
    private final String blockName;
    private final String blockCode;

    public Block(long blockId, long hostelId,
                 String blockName, String blockCode) {
        this.blockId = blockId;
        this.hostelId = hostelId;
        this.blockName = blockName;
        this.blockCode = blockCode;
    }

    public long getBlockId() {
        return blockId;
    }

    public long getHostelId() {
        return hostelId;
    }

    public String getBlockName() {
        return blockName;
    }

    public String getBlockCode() {
        return blockCode;
    }
}