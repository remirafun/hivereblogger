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
package simpleserver.log;

public class Log {    
    public static void log(Object b0) {
        String clsName = Thread.currentThread().getStackTrace()[2].toString();
        System.out.print(b0);
        System.out.print("\t\t at ");
        System.out.println(clsName);
    }
    public static void log(Object b0, Object b1) {
        String clsName = Thread.currentThread().getStackTrace()[2].toString();
        System.out.print(b0);
        System.out.print(b1);
        System.out.print("\t\t at ");
        System.out.println(clsName);
    }
    public static void err(Throwable b0) {
        b0.printStackTrace(System.err);
    }
    public static void err(Object b0) {
        String clsName = Thread.currentThread().getStackTrace()[2].toString();
        System.err.print(b0);
        System.err.print("\t\t at ");
        System.err.println(clsName);
    }
    public static void err(Object b0, Object b1) {
        String clsName = Thread.currentThread().getStackTrace()[2].toString();
        System.err.print(b0);
        System.err.print(b1);
        System.err.print("\t\t at ");
        System.err.println(clsName);
    }
    public static void err(Object b0, Object b1, Object b2) {
        String clsName = Thread.currentThread().getStackTrace()[2].toString();
        System.err.print(b0);
        System.err.print(b1);
        System.err.print(b2);
        System.err.print("\t\t at ");
        System.err.println(clsName);
    }
    public static void err(Object b0, Object b1, Object b2, Object b3) {
        String clsName = Thread.currentThread().getStackTrace()[2].toString();
        System.err.print(b0);
        System.err.print(b1);
        System.err.print(b2);
        System.err.print(b3);
        System.err.print("\t\t at ");
        System.err.println(clsName);
    }
}
