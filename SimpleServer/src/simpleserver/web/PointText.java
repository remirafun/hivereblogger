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
package simpleserver.web;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Random;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

public class PointText {
    public static class BitSetLayer {
        public char[] arr;
        int w, h;
        public BitSet mem;

        public BitSetLayer(int w, int h) {
            this.w = w;
            this.h = h;
            this.mem = new BitSet(w*h);
        }
        public boolean isSet(int x, int y) {
            return mem.get(x*h+y);
        }
        public void set(int x, int y) {
            mem.set(x*h+y);
        }
        public boolean setIfInBounds(int x, int y) {
            if(x >= 0 && x < w && y >= 0 && y < h) {
                mem.set(x*h+y);
                return true;
            }
            return false;
        } 
        public boolean setIfInBounds3(int x, int y) {
            if(x >= 1 && x+1 < w && y >= 1 && y+1 < h) {
                int a = x*h+y;
                mem.set(a-1, a+1);
                a = (x-1)*h+y;
                mem.set(a-1, a+1);
                a = (x+1)*h+y;
                mem.set(a-1, a+1);
                return true;
            }
            return false;
        } 
        public void print() {
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    System.out.print(isSet(x, y)?'#':' ');
                }
                System.out.println();
            }
        }
        
    }
    public static class Letter {
        char ch;
        int line;
        int w, h;
        
        int offset;
        BitSet mem;
        
        public boolean isSet(int x, int y) {
            return mem.get(x*h+y);
        }
        public void print() {
            for(int y = 0; y < h; y++) {
                for(int x = 0; x < w; x++) {
                    System.out.print(isSet(x, y)?'#':' ');
                }
                System.out.println();
            }
        }
        public void render(Random r, BitSetLayer g, int x0, int y0) {
            double cosOffset = r.nextDouble()*Math.PI*2;
            float cos = 4;
            float cos2 = 8;
            int gridSize = 2;
            //int half = gridSize>>1;
            int items = 0;
            for(int x = 0; x < w; x++) {
                for(int y = 0; y < h; y++) {
                    if(isSet(x, y)) {
                        int rx = gridSize*x+r.nextInt(gridSize);
                        int ry = gridSize*(line+y)+r.nextInt(gridSize);
                        ry = (int)(ry+Math.cos(cosOffset+rx*cos*0.01f)*cos2);
                        
                        int xx = (rx+x0);
                        int yy = (ry+y0);
                        if(g.setIfInBounds3(xx, yy)) {
                            items++;
                        }
                        
//                        draw(rx, ry, half, diamSize, fill, cos, cos2);
                    }
                }
            }            
        }
    }
    
    public static Letter[] letters;
    public static HashMap<Character, Letter> letterMap;
    
    public static Letter[] genLetters() {
//        char[] arr = "abcdefghijklmnopqrstuvwxyz0123456789?@".toCharArray();
        char[] arr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789?@".toCharArray();
//        System.out.println("array " + arr.length);
        Font font = new Font(Font.SERIF, Font.BOLD|Font.ITALIC, 72);
        
        Letter[] arr2 = new Letter[arr.length];
        
        BufferedImage i = new BufferedImage(100, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g =  i.createGraphics();
        for(int m = 0; m < arr.length; m++) {
            g.setColor(Color.white);
            g.fillRect(0, 0, i.getWidth(), i.getHeight());

            g.setFont(font);
            g.setColor(Color.black);
            g.drawString(""+arr[m], 16, 80);

            int items = 0;
            int gridSize = 4;
            int w = i.getWidth();
            int h = i.getHeight();

            int minW = i.getWidth()+1, maxW = -1;
            int minH = i.getHeight()+1, maxH = -1;

            ArrayList<Point> list = new ArrayList<>();
            for(int y = 0, gy = 0; y < h; y+=gridSize, gy++) {
                loop:
                for(int x = 0, gx = 0; x < w; x+=gridSize, gx++) {
                    for(int yy = 0; yy < gridSize && y+yy < h; yy++) {
                        int rgb = i.getRGB(x, y+yy);
                        if((rgb&0xff) == 0) {
                            items++;
                            minW = Math.min(minW, gx);
                            maxW = Math.max(maxW, gx);
                            minH = Math.min(minH, gy);
                            maxH = Math.max(maxH, gy);
                            list.add(new Point(gx, gy));
//                            System.out.print("#");
                            continue loop;
                        }
                    }
//                    System.out.print(" ");
                }
//                System.out.println();
            }
//            System.out.println("items " + items + " " + minW + " " + maxW + "    " + minH + " " + maxH);

            int ww = maxW-minW+1;
            int hh = maxH-minH+1;
            int bits = ww*hh;

            int DEFAULT_SET_SIZE = 8192;
            int setOffset = 0;
            BitSet set = new BitSet(DEFAULT_SET_SIZE);

            if(setOffset+bits >= set.size()) {
                set = new BitSet(DEFAULT_SET_SIZE);
                setOffset = 0;
            }

            Letter l = new Letter();
            l.ch = arr[m];
            l.offset = setOffset;
            l.mem = set;
            l.w = ww;
            l.h = hh;
            l.line = minH;

            for(Point p : list) {
                set.set(setOffset+(p.x-minW)*hh+(p.y-minH));
            }
            arr2[m] = l;
            setOffset += bits;

        }
        g.dispose();
//        l.print();
        return arr2;
    }
    static BitSetLayer layer;
    public static Random rnd;
    static {
        initLetters();
    }
    public static void initLetters() {
        if(letters == null) {
            letters = genLetters();
            letterMap = new HashMap<>();
            for(Letter l : letters) letterMap.put(l.ch, l);

            layer = new BitSetLayer(200, 60);
            rnd = new Random();
            layer.arr = new char[6];
        }
    }
    public static BitSetLayer render() {
        layer.mem.clear();
        renderRandom(layer, rnd, rnd.nextBoolean()?5:6);
        return layer;
    } 
    public static void rnd(byte[] b) {
        int num = 6;
        for(int i = 0; i < num; i++) {
            Letter l = letters[rnd.nextInt(letters.length)];
            b[i] = (byte)l.ch;
        }
        b[0] = (byte)'+';
    }
    public static void renderRandom(BitSetLayer b, Random r, int num) {
        int x0 = 4;
        int i = 0;
        for(; i < num; i++) {
            Letter l = letters[r.nextInt(letters.length)];
            b.arr[i] = l.ch;
            l.render(r, b, x0, 0);
            x0 += (l.w<<1);
        }
        for(; i < b.arr.length; i++) b.arr[i] = 0;
    }
}
