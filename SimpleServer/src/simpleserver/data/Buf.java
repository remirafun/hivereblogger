/*
 * Copyright (c) 2021, mirafun
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the copyright holder nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package simpleserver.data;

import java.nio.ByteBuffer;
import simpleserver.log.Log;

public class Buf implements AutoCloseable {
    public static Buf wrap(ByteBuffer buf) {
        return new Buf(buf);
    }
    public static Buf buf(ByteBuffer buf, int size) {
        return new Buf(buf, size);
    }
    public ByteBuffer data;
    public Object obj;
   
    public int refCount() { return 0; }
    public void refCount(int i) { }
    public void retain() {}
    public void release() {}

    @Override
    public void close() {
        release();
    }
    
    
    public Buf(ByteBuffer buf) {
        this.data = buf;
    }
	private Buf(ByteBuffer buf, int size) {
		this.data = buf;
//		this.dataU8 = new Uint8Array(buf);
//		this.pos = 0;
//		this.limit = size;
//		this.capacity = size;
	}
    
	public void dump() {
		var str = "";
		for(var i = 0; i < data.limit(); i++) {
			str += data.get(i) + " ";
		}
		Log.log(str);
	}
    public int pos() { return data.position(); }
    public int limit() { return data.limit(); }
	public Buf clear() { /*this.pos = 0; this.limit = this.capacity;*/ data.clear(); return this; }
	public Buf flip() { /*this.limit = this.pos; this.pos = 0; var v = this.dataU8.subarray(this.pos,this.limit); v.buf = this;*/ data.flip(); return this;}
	public void rewind() { data.rewind(); /*this.pos = 0;*/ }
	public void checkSize(int x, int size) {
		if(x < 0 || x >= size) throw new IllegalArgumentException("Out of bounds " + x + " " + " " + size);
	}	
    public Buf putByte(int index, int b) {
		this.checkSize(b, 256);
//		this.checkSize(this.pos, this.limit);
//		this.dataU8[this.pos++] = b;
        data.put(index, (byte)b);
		return this;
	}
    public Buf putByte(byte b) {
        data.put(b);
		return this;
    }
	public Buf putByte(int b) {
		this.checkSize(b, 256);
//		this.checkSize(this.pos, this.limit);
//		this.dataU8[this.pos++] = b;
        data.put((byte)b);
		return this;
	}
	public int getByte() {
//		this.checkSize(this.pos, this.limit);
//		return this.dataU8[this.pos++];
        return data.get()&0xff;
	}
    public int getByte(int pos) {
//		this.checkSize(this.pos, this.limit);
//		return this.dataU8[this.pos++];
        return data.get(pos)&0xff;
	}
	public Buf putChar(int ch) {
		this.checkSize(ch, 65536);
//		this.checkSize(this.pos+1, this.limit);
//		tmp_arrayBufU16[0] = ch;
//		if(isBigEndian) {
//			this.dataU8[this.pos++] = tmp_arrayBufU8[0];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[1];
//		}
//		else {
//			this.dataU8[this.pos++] = tmp_arrayBufU8[1];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[0];
//		}
        data.putChar((char)ch);
		return this;
	}
	public char getChar() {
//		this.checkSize(this.pos+1, this.limit);
//		if(isBigEndian) {
//			tmp_arrayBufU8[0] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[1] = this.dataU8[this.pos++];
//		}
//		else {
//			tmp_arrayBufU8[1] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[0] = this.dataU8[this.pos++];
//		}
//		return tmp_arrayBufU16[0];
        return data.getChar();
	}
	public char peekChar() {
//		this.checkSize(this.pos+1, this.limit);
//		if(isBigEndian) {
//			tmp_arrayBufU8[0] = this.dataU8[this.pos];
//			tmp_arrayBufU8[1] = this.dataU8[this.pos+1];
//		}
//		else {
//			tmp_arrayBufU8[1] = this.dataU8[this.pos];
//			tmp_arrayBufU8[0] = this.dataU8[this.pos+1];
//		}
//		return tmp_arrayBufU16[0];
        return data.getChar(data.position());
	}
    public Buf putInt(int ch) {
        data.putInt(ch);
		return this;
    }
    public int getInt() {
        return data.getInt();
    }
	public Buf putFloat(float ch) {
//		this.checkSize(this.pos+3, this.limit);
//		tmp_arrayBufF32[0] = ch;
//		if(isBigEndian) {
//			this.dataU8[this.pos++] = tmp_arrayBufU8[0];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[1];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[2];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[3];
//		}
//		else {
//			this.dataU8[this.pos++] = tmp_arrayBufU8[3];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[2];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[1];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[0];
//		}
        data.putFloat(ch);
		return this;
	}
	public float getFloat() {
//		this.checkSize(this.pos+3, this.limit);
//		if(isBigEndian) {
//			tmp_arrayBufU8[0] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[1] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[2] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[3] = this.dataU8[this.pos++];
//		}
//		else {
//			tmp_arrayBufU8[3] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[2] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[1] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[0] = this.dataU8[this.pos++];
//		}
		return data.getFloat();
	}
    public static final long MAX_SAFE_LONG = 9007199254740991L;
	public Buf putJsLong(long ch) {
        if(ch < -MAX_SAFE_LONG || ch > MAX_SAFE_LONG) {
            throw new IllegalArgumentException("Long value too large " + ch);
        }
//		this.checkSize(this.pos+7, this.limit);
//		tmp_arrayBufI64[0] = BigInt(ch);
//		if(isBigEndian) {
//			this.dataU8[this.pos++] = tmp_arrayBufU8[0];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[1];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[2];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[3];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[4];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[5];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[6];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[7];
//		}
//		else {	
//			this.dataU8[this.pos++] = tmp_arrayBufU8[7];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[6];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[5];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[4];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[3];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[2];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[1];
//			this.dataU8[this.pos++] = tmp_arrayBufU8[0];
//		}
//		return this;
        data.putLong(ch);
        return this;
	}
	public long getLong() {
//		this.checkSize(this.pos+7, this.limit);
//		if(isBigEndian) {
//			tmp_arrayBufU8[0] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[1] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[2] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[3] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[4] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[5] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[6] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[7] = this.dataU8[this.pos++];
//		}
//		else {
//			tmp_arrayBufU8[7] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[6] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[5] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[4] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[3] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[2] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[1] = this.dataU8[this.pos++];
//			tmp_arrayBufU8[0] = this.dataU8[this.pos++];
//		}
//		return Number(tmp_arrayBufI64[0]);
        return data.getLong();
	}
    
	public Buf putByteString(String str) {
		var len = str.length();
//		this.checkSize(this.pos+1+len, this.limit);
		this.putChar(len);
		for(var i = 0; i < len; i++) {
			this.putByte((byte)str.charAt(i));
		}
		return this;
	}
	public String getByteString() {
//		this.checkSize(this.pos+1, this.limit);
		var len = this.getChar();
//		this.checkSize(this.pos-1+len, this.limit);
		StringBuilder res = new StringBuilder(Math.min(len, data.remaining()));
		for(var i = 0; i < len; i++) {
			res.append((char)this.getByte());
		}
		return res.toString();
	}
    public String peekByteString() {
//		this.checkSize(this.pos+1, this.limit);
		var len = this.peekChar();
//		this.checkSize(this.pos-1+len, this.limit);
		StringBuilder res = new StringBuilder(Math.min(len, data.remaining()));
        int p = pos()+2;
		for(var i = 0; i < len; i++) {
			res.append((char)this.getByte(p+i));
		}
		return res.toString();
	}
	public Buf putString(String str) {
		var len = str.length();
//		this.checkSize(this.pos+1+len+len, this.limit);
		this.putChar(len);
		for(var i = 0; i < len; i++) {
			this.putChar(str.charAt(i));
		}
		return this;
	}
	public String getString() {
//		this.checkSize(this.pos+1, this.limit);
		var len = this.getChar();
//		this.checkSize(this.pos-1+len+len, this.limit);
		StringBuilder res = new StringBuilder(Math.min(len, data.remaining()>>1));
		for(var i = 0; i < len; i++) {
            res.append(this.getChar());
		}
		return res.toString();
	}
//	public Buf putObj(Object obj) {
//		this.putString(JSON.stringify(obj));
//		return this;
//	}
//	public Object getObj() {
//		var str = this.getString();
//		return JSON.parse(str);
//	}
}
