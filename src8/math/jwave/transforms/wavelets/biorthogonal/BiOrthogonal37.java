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
 * BiOrthogonal Wavelet of type 3.7 - Three vanishing moments in wavelet
 * function and seven vanishing moments in scaling function.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 17:21:28
 */
public class BiOrthogonal37 extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/bior3.7/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 17:21:28
   */
  public BiOrthogonal37( ) {

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 16; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];
    _scalingDeCom[ 0 ] = 0.0030210861012608843;
    _scalingDeCom[ 1 ] = -0.009063258303782653;
    _scalingDeCom[ 2 ] = -0.01683176542131064;
    _scalingDeCom[ 3 ] = 0.074663985074019;
    _scalingDeCom[ 4 ] = 0.03133297870736289;
    _scalingDeCom[ 5 ] = -0.301159125922835;
    _scalingDeCom[ 6 ] = -0.026499240945345472;
    _scalingDeCom[ 7 ] = 0.9516421218971786;
    _scalingDeCom[ 8 ] = 0.9516421218971786;
    _scalingDeCom[ 9 ] = -0.026499240945345472;
    _scalingDeCom[ 10 ] = -0.301159125922835;
    _scalingDeCom[ 11 ] = 0.03133297870736289;
    _scalingDeCom[ 12 ] = 0.074663985074019;
    _scalingDeCom[ 13 ] = -0.01683176542131064;
    _scalingDeCom[ 14 ] = -0.009063258303782653;
    _scalingDeCom[ 15 ] = 0.0030210861012608843;

    _waveletDeCom = new double[ _motherWavelength ];
    _waveletDeCom[ 0 ] = 0.;
    _waveletDeCom[ 1 ] = 0.;
    _waveletDeCom[ 2 ] = 0.;
    _waveletDeCom[ 3 ] = 0.;
    _waveletDeCom[ 4 ] = 0.;
    _waveletDeCom[ 5 ] = 0.;
    _waveletDeCom[ 6 ] = -0.1767766952966369;
    _waveletDeCom[ 7 ] = 0.5303300858899107;
    _waveletDeCom[ 8 ] = -0.5303300858899107;
    _waveletDeCom[ 9 ] = 0.1767766952966369;
    _waveletDeCom[ 10 ] = 0.;
    _waveletDeCom[ 11 ] = 0.;
    _waveletDeCom[ 12 ] = 0.;
    _waveletDeCom[ 13 ] = 0.;
    _waveletDeCom[ 14 ] = 0.;
    _waveletDeCom[ 15 ] = 0.;

    // build all other coefficients from low & high pass decomposition
    _buildBiOrthonormalSpace( );

  } // BiOrthogonal37
  
} // BiOrthogonal37