package io.github.oaschi.drownforce;

import org.bukkit.block.Block;

public class Drown {
	
	private Block block;
	private boolean forever;
	private int remainingAir;
	
	public Drown(boolean forever){
		this.forever = forever;
	}
	
	public Drown(){
		this(false);
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public boolean isForever() {
		return forever;
	}

	public void setForever(boolean forever) {
		this.forever = forever;
	}

	public int getRemainingAir() {
		return remainingAir;
	}

	public void setRemainingAir(int breath) {
		this.remainingAir = breath;
	}

}
