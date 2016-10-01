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
package system.os.mslinks.data;

import system.os.mslinks.Serializable;
import system.os.mslinks.io.ByteReader;
import system.os.mslinks.io.ByteWriter;

import java.io.IOException;

public class BitSet32 implements Serializable {
	private int d;
	
	BitSet32(int n) {
		d = n;
	}
	
	BitSet32(ByteReader data) throws IOException {
		d = (int)data.read4bytes();
	}
	
	boolean get(int i) {
		return (d & (1 << i)) != 0;
	}
	
	void set(int i) {
		d = (d & ~(1 << i)) | (1 << i);
	}
	
	void clear(int i) {
		d = d & ~(1 << i);
	}

	public void serialize(ByteWriter bw) throws IOException {
		bw.write4bytes(d);		
	}
}
