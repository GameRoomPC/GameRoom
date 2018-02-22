/*
	https://github.com/BlackOverlord666/mslinks
	
	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package com.gameroom.system.os.mslinks.data;

import com.gameroom.system.os.mslinks.io.ByteReader;

import java.io.IOException;

public class CNRLinkFlags extends BitSet32 {
	
	public CNRLinkFlags(int n) {
		super(n);
		reset();
	}

	public CNRLinkFlags(ByteReader data) throws IOException {
		super(data);
		reset();
	}
	
	private void reset() {
		for (int i=2; i<32; i++)
			clear(i);
	}
	
	public boolean isValidDevice() 		{ return get(0); }
	public boolean isValidNetType()		{ return get(1); }
	
	public CNRLinkFlags setValidDevice() 		{ set(0); return this; }	
	public CNRLinkFlags setValidNetType()		{ set(1); return this; }
	
	public CNRLinkFlags clearValidDevice() 		{ clear(0); return this; }	
	public CNRLinkFlags clearValidNetType()		{ clear(1); return this; }

}
