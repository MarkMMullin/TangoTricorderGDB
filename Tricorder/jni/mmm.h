/*
 * mmm.h
 *
 *  Created on: Apr 7, 2015
 *      Author: mark
 */

#ifndef MMM_H_
#define MMM_H_

//! macro to delete and zero a pointer
#define SAFE_DELETE(p)  { if(p) { delete (p);     (p)=NULL; } }
//! macro to delete and zero an array pointer
#define SAFE_DELETE_ARRAY(p) { if(p) {delete[] (p); (p) = NULL; }}

#endif /* MMM_H_ */
