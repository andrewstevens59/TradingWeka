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
package math.jwave.transforms.wavelets;

/**
 * Discrete Mayer (FIR approximation) filter: symmetric, orthogonal
 * (orthonormal), biorthogonal.
 * 
 * @author Christian Scheiblich (cscheiblich@gmail.com)
 * @date 16.02.2014 13:58:12
 */
public class DiscreteMayer extends Wavelet {

  /**
   * Already orthonormal coefficients taken from Filip Wasilewski's webpage
   * http://wavelets.pybytes.com/wavelet/dmey/ Thanks!
   * 
   * @author Christian Scheiblich (cscheiblich@gmail.com)
   * @date 16.02.2014 13:58:12
   */
  public DiscreteMayer( ) {

    _transformWavelength = 2; // minimal wavelength of input signal

    _motherWavelength = 62; // wavelength of mother wavelet

    _scalingDeCom = new double[ _motherWavelength ];

    _scalingDeCom[ 0 ] = 0.;
    _scalingDeCom[ 1 ] = -1.009999956941423e-12;
    _scalingDeCom[ 2 ] = 8.519459636796214e-09;
    _scalingDeCom[ 3 ] = -1.111944952595278e-08;
    _scalingDeCom[ 4 ] = -1.0798819539621958e-08;
    _scalingDeCom[ 5 ] = 6.066975741351135e-08;
    _scalingDeCom[ 6 ] = -1.0866516536735883e-07;
    _scalingDeCom[ 7 ] = 8.200680650386481e-08;
    _scalingDeCom[ 8 ] = 1.1783004497663934e-07;
    _scalingDeCom[ 9 ] = -5.506340565252278e-07;
    _scalingDeCom[ 10 ] = 1.1307947017916706e-06;
    _scalingDeCom[ 11 ] = -1.489549216497156e-06;
    _scalingDeCom[ 12 ] = 7.367572885903746e-07;
    _scalingDeCom[ 13 ] = 3.20544191334478e-06;
    _scalingDeCom[ 14 ] = -1.6312699734552807e-05;
    _scalingDeCom[ 15 ] = 6.554305930575149e-05;
    _scalingDeCom[ 16 ] = -0.0006011502343516092;
    _scalingDeCom[ 17 ] = -0.002704672124643725;
    _scalingDeCom[ 18 ] = 0.002202534100911002;
    _scalingDeCom[ 19 ] = 0.006045814097323304;
    _scalingDeCom[ 20 ] = -0.006387718318497156;
    _scalingDeCom[ 21 ] = -0.011061496392513451;
    _scalingDeCom[ 22 ] = 0.015270015130934803;
    _scalingDeCom[ 23 ] = 0.017423434103729693;
    _scalingDeCom[ 24 ] = -0.03213079399021176;
    _scalingDeCom[ 25 ] = -0.024348745906078023;
    _scalingDeCom[ 26 ] = 0.0637390243228016;
    _scalingDeCom[ 27 ] = 0.030655091960824263;
    _scalingDeCom[ 28 ] = -0.13284520043622938;
    _scalingDeCom[ 29 ] = -0.035087555656258346;
    _scalingDeCom[ 30 ] = 0.44459300275757724;
    _scalingDeCom[ 31 ] = 0.7445855923188063;
    _scalingDeCom[ 32 ] = 0.44459300275757724;
    _scalingDeCom[ 33 ] = -0.035087555656258346;
    _scalingDeCom[ 34 ] = -0.13284520043622938;
    _scalingDeCom[ 35 ] = 0.030655091960824263;
    _scalingDeCom[ 36 ] = 0.0637390243228016;
    _scalingDeCom[ 37 ] = -0.024348745906078023;
    _scalingDeCom[ 38 ] = -0.03213079399021176;
    _scalingDeCom[ 39 ] = 0.017423434103729693;
    _scalingDeCom[ 40 ] = 0.015270015130934803;
    _scalingDeCom[ 41 ] = -0.011061496392513451;
    _scalingDeCom[ 42 ] = -0.006387718318497156;
    _scalingDeCom[ 43 ] = 0.006045814097323304;
    _scalingDeCom[ 44 ] = 0.002202534100911002;
    _scalingDeCom[ 45 ] = -0.002704672124643725;
    _scalingDeCom[ 46 ] = -0.0006011502343516092;
    _scalingDeCom[ 47 ] = 6.554305930575149e-05;
    _scalingDeCom[ 48 ] = -1.6312699734552807e-05;
    _scalingDeCom[ 49 ] = 3.20544191334478e-06;
    _scalingDeCom[ 50 ] = 7.367572885903746e-07;
    _scalingDeCom[ 51 ] = -1.489549216497156e-06;
    _scalingDeCom[ 52 ] = 1.1307947017916706e-06;
    _scalingDeCom[ 53 ] = -5.506340565252278e-07;
    _scalingDeCom[ 54 ] = 1.1783004497663934e-07;
    _scalingDeCom[ 55 ] = 8.200680650386481e-08;
    _scalingDeCom[ 56 ] = -1.0866516536735883e-07;
    _scalingDeCom[ 57 ] = 6.066975741351135e-08;
    _scalingDeCom[ 58 ] = -1.0798819539621958e-08;
    _scalingDeCom[ 59 ] = -1.111944952595278e-08;
    _scalingDeCom[ 60 ] = 8.519459636796214e-09;
    _scalingDeCom[ 61 ] = -1.009999956941423e-12;

    _buildOrthonormalSpace( );

  } // DiscreteMayer

} // DiscreteMayer