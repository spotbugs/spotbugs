/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.gui2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Panel;
import java.awt.Toolkit;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/*
 *  If long load times are getting you down, uncomment this class's instantiation in driver 
 * and add an extra 4 seconds to your load time, but get to watch a dancing bug for the remainder!
*/

public class SplashFrame extends JWindow
{
	
	private static Thread animator;
	
	public SplashFrame()
	{
		super(new Frame());
		
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image image = toolkit.getImage(MainFrame.class.getResource("SplashBug1.png"));
		Image image2 = toolkit.getImage(MainFrame.class.getResource("SplashBug2B.png"));
		Image imageReverse = toolkit.getImage(MainFrame.class.getResource("SplashBug1reverse.png"));
		Image image2Reverse = toolkit.getImage(MainFrame.class.getResource("SplashBug2reverseB.png"));
		
		//		JPanel temp = new JPanel(new BorderLayout());
//		temp.setBorder(BorderFactory.createLineBorder(Color.black, 2));
//		setContentPane(temp);
		JLabel l = new JLabel(new ImageIcon(MainFrame.class.getResource("umdFindbugs.png")));
		JPanel p = new JPanel();
		Viewer viewer=new Viewer(image,image2,imageReverse,image2Reverse);
		final JPanel bottom = viewer;
		p.setBackground(Color.white);
		bottom.setBackground(Color.white);
		
		p.add(l);
		getContentPane().add(p, BorderLayout.CENTER);
		getContentPane().add(bottom, BorderLayout.SOUTH);
		pack();
		Dimension screenSize =
			Toolkit.getDefaultToolkit().getScreenSize();
		Dimension labelSize = l.getPreferredSize();
		p.setPreferredSize(new Dimension(labelSize.width + 50, labelSize.height + 20));
		p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		bottom.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		Dimension panelSize = p.getPreferredSize();
		bottom.setPreferredSize(new Dimension(panelSize.width, image.getHeight(null)+2));
		
		setLocation(screenSize.width/2 - (panelSize.width/2),
				screenSize.height/2 - (panelSize.height/2));
		
//		g.drawImage(new ImageIcon("bugSplash3.png"),0 ,0 ,null);
		
		pack();
		screenSize = null;
		labelSize = null;
		viewer.animate();
		
		
	}
	
	public static void main(String[] args){
		(new SplashFrame()).setVisible(true);
		
	}
	
	@Override
    public void setVisible(boolean b){
		super.setVisible(b);
		if(!b)
			animator.interrupt();			
	}
	
	private static class Viewer extends JPanel {
		private Image image;
		private Image image2;
		private Image imageR;
		private Image image2R;
		boolean swap=false;
		boolean reverse=true;
		int callCount=0;
		int xpos=0;
		int ypos=0;
		int farRight;
		
		public Viewer(Image i1,Image i2,Image i1r,Image i2r) {
			image=i1;
			image2=i2;
			imageR=i1r;
			image2R=i2r;
			MediaTracker mediaTracker = new MediaTracker(this);
			mediaTracker.addImage(image, 0);
			mediaTracker.addImage(image2, 1);
			mediaTracker.addImage(imageR, 2);
			mediaTracker.addImage(image2R, 3);
			try
			{
				mediaTracker.waitForID(0);
				mediaTracker.waitForID(1);
				mediaTracker.waitForID(2);
				mediaTracker.waitForID(3);
			}
			catch (InterruptedException ie)
			{
				System.err.println(ie);
				System.exit(1);
			}
			animator = new Thread(new Runnable()
			{
				public void run()
				{
					int deltaX=1;
					
					while(true)
					{
						if(Thread.currentThread().isInterrupted())
							return;
						
						callCount++;
						if (callCount==10)
						{
							swap=!swap;
							callCount=0;
						}

						xpos+=deltaX;
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							break;
						}

						if (xpos>Viewer.this.getSize().width-image.getWidth(null))
						{
							deltaX=-1;
							reverse=!reverse;
						}
						if (xpos<0)
						{
							deltaX=1;
							reverse=!reverse;
						}
						
						Viewer.this.repaint();
					}
				}
			});
		}
		
		public void animate()
		{
			animator.start();
		}
		
		@Override
        public void setPreferredSize(Dimension d)
		{
			super.setPreferredSize(d);
		}
		private Image imageToDraw()
		{
			if (swap)
			{
				if (!reverse)
					return image;
				return imageR;
			}
			else
			{
				if (!reverse)
					return image2;
				return image2R;
			}
		}
		
		@Override
        public void paint(Graphics graphics) {
			super.paint(graphics);
			
//			graphics.clearRect(0,0,500,500);
			graphics.drawImage(imageToDraw(), xpos, ypos, null);
		}
	}
}