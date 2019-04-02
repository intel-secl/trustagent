/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package gov.niarl.his.privacyca;
import java.util.*;
import java.net.InetAddress;

/*
 * win : return 0;
 * nix/nux: return 1;
 * other: return -1
 */
public class IdentityOS {
	
//	public static void main(String[] args){
//		System.out.println("dgag");
//	}
	
	public static int osType(){
		if(isWindows()){
//			System.out.println("This is Windows");
			return 0;
		}else if(isUnix()){
//			System.out.println("This is Unix or Linux");
			return 1;
		}else{
//			System.out.println("Your OS is not support!!");
			return -1;
		}
	}
 
	public static boolean isWindows(){
 
		String os = System.getProperty("os.name").toLowerCase();
		//windows
	    return (os.indexOf( "win" ) >= 0); 
 
	}
 
	public static boolean isUnix(){
 
		String os = System.getProperty("os.name").toLowerCase();
		//linux or unix
	    return (os.indexOf( "nix") >=0 || os.indexOf( "nux") >=0);
 
	}


}
