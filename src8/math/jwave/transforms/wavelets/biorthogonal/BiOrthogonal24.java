/**
 * JWave - Java implementation of wavelet transform algorithms
 *
 * Copyright 2008-2014 Christian Scheiblich
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * This file is part of JWave.
 *
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 23.05.2008 17:42:23
 *
 */
package math.jwave.transforms.wavelets.biorthogonal;

import math.jwave.transforms.wavelets.Wavelet;

/**
 * BiOrthogonal Wavelet of type 2.4 - Two vanishing moments in wavelet function
 * and four vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 16:24:22
 */
public class BiOrthogonal24 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior2.4/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 16:24:22
   */
  public BiOrthogonal24( ) {

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 10; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = 0.03314563036811942;
    _scalingDeCom[ 2 ] = -0.06629126073623884;
    _scalingDeCom[ 3 ] = -0.1767766952966369;
    _scalingDeCom[ 4 ] = 0.4198446513295126;
    _scalingDeCom[ 5 ] = 0.9943689110435825;
    _scalingDeCom[ 6 ] = 0.4198446513295126;
    _scalingDeCom[ 7 ] = -0.1767766952966369;
    _scalingDeCom[ 8 ] = -0.06629126073623884;
    _scalingDeCom[ 9 ] = 0.03314563036811942;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.;
    _waveletDeCom[ 2 ] = 0.;
    _waveletDeCom[ 3 ] = 0.3535533905932738;
    _waveletDeCom[ 4 ] = -0.7071067811865476;
    _waveletDeCom[ 5 ] = 0.3535533905932738;
    _waveletDeCom[ 6 ] = 0.;
    _waveletDeCom[ 7 ] = 0.;
    _waveletDeCom[ 8 ] = 0.;
    _waveletDeCom[ 9 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal24

} // BiOrthogonal24