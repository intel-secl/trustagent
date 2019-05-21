/*
 * Copyright (C) 2019 Intel Corporation
 * SPDX-License-Identifier: BSD-3-Clause
 */
package gov.niarl.his.privacyca;

/*
 * win : return 0;
 * nix/nux: return 1;
 * other: return -1
 */
public class IdentityOS {
	public static int osType(){
		if(isWindows()){
			return 0;
		}else if(isUnix()){
			return 1;
		}else{
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
