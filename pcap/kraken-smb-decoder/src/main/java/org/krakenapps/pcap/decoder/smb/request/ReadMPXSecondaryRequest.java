package org.krakenapps.pcap.decoder.smb.request;
import org.krakenapps.pcap.decoder.smb.structure.SmbData;

public class ReadMPXSecondaryRequest implements SmbData{
// this is not use
	boolean malformed = false;
	@Override
	public boolean isMalformed() {
		// TODO Auto-generated method stub
		return malformed;
	}
	@Override
	public void setMalformed(boolean malformed) {
		this.malformed = malformed;
	}
	@Override
	public String toString(){
		return String.format("First level : Read MPX Secondary Request\n" +
				"isMalformed = %s\n",
				this.malformed);
	}
}
