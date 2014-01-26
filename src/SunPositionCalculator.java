
public class SunPositionCalculator {

	public static final double pi = 3.14159265358979323846;
	public static final double rad = pi/180;
	public static final double dEarthMeanRadius = 6371.01; // in km
	public static final int dAstronomicalUnit = 149597890; // in km
	
	public double m_dZenithAngle;
	public double m_dAzimuth;
	
	public SunPositionCalculator()
	{
		m_dZenithAngle = 0;
		m_dAzimuth = 0;
	}
	
	public double GetZenith()
	{
		return m_dZenithAngle;
	}
	
	public double GetAzimuth()
	{
		return m_dAzimuth;
	}
	
	// The following algorithm is a java translation of this: http://www.psa.es/sdg/sunpos.htm
	public void calculateSunPos(int iYear, int iMonth, int iDay, double dHours, double dMinutes, double dSeconds,
							  double dLongitude, double dLatitude)
	{
		// Main variables
		double dElapsedJulianDays;
		double dDecimalHours;
		double dEclipticLongitude;
		double dEclipticObliquity;
		double dRightAscension;
		double dDeclination;

		// Auxiliary variables
		double dY;
		double dX;

		// Calculate difference in days between the current Julian Day
		// and JD 2451545.0, which is noon 1 January 2000 Universal Time
		{
			double dJulianDate;
			int liAux1;
			int liAux2;
			// Calculate time of the day in UT decimal hours
			dDecimalHours = dHours + (dMinutes + dSeconds / 60.0) / 60.0;
			// Calculate current Julian Day
			liAux1 = (iMonth - 14) / 12;
			liAux2 = (1461 * (iYear + 4800 + liAux1)) / 4 + (367 * (iMonth - 2 - 12 * liAux1)) / 12 - (3 * ((iYear + 4900 + liAux1) / 100)) / 4 + iDay - 32075;
			dJulianDate = (double)(liAux2) - 0.5 + dDecimalHours / 24.0;
			// Calculate difference between current Julian Day and JD 2451545.0
			dElapsedJulianDays = dJulianDate-2451545.0;
		}

		// Calculate ecliptic coordinates (ecliptic longitude and obliquity of the
		// ecliptic in radians but without limiting the angle to be less than 2*Pi
		// (i.e., the result may be greater than 2*Pi)
		{
			double dMeanLongitude;
			double dMeanAnomaly;
			double dOmega;
			dOmega = 2.1429 - 0.0010394594 * dElapsedJulianDays;
			dMeanLongitude = 4.8950630 + 0.017202791698 * dElapsedJulianDays; // Radians
			dMeanAnomaly = 6.2400600 + 0.0172019699 * dElapsedJulianDays;
			dEclipticLongitude = dMeanLongitude + 0.03341607 * Math.sin(dMeanAnomaly) + 0.00034894 * Math.sin(2 * dMeanAnomaly) - 0.0001134 - 0.0000203 * Math.sin(dOmega);
			dEclipticObliquity = 0.4090928 - 6.2140e-9 * dElapsedJulianDays + 0.0000396 * Math.cos(dOmega);
		}

		// Calculate celestial coordinates ( right ascension and declination ) in radians
		// but without limiting the angle to be less than 2*Pi (i.e., the result may be
		// greater than 2*Pi)
		{
			double dSin_EclipticLongitude;
			dSin_EclipticLongitude = Math.sin(dEclipticLongitude);
			dY = Math.cos(dEclipticObliquity) * dSin_EclipticLongitude;
			dX = Math.cos(dEclipticLongitude);
			dRightAscension = Math.atan2(dY,dX);
			if (dRightAscension < 0.0)
				dRightAscension = dRightAscension + 2 * pi;
			dDeclination = Math.asin(Math.sin(dEclipticObliquity) * dSin_EclipticLongitude);
		}

		// Calculate local coordinates ( azimuth and zenith angle ) in degrees
		{
			double dGreenwichMeanSiderealTime;
			double dLocalMeanSiderealTime;
			double dLatitudeInRadians;
			double dHourAngle;
			double dCos_Latitude;
			double dSin_Latitude;
			double dCos_HourAngle;
			double dParallax;
			dGreenwichMeanSiderealTime = 6.6974243242 + 0.0657098283 * dElapsedJulianDays + dDecimalHours;
			dLocalMeanSiderealTime = (dGreenwichMeanSiderealTime * 15 + dLongitude) * pi / 180;
			dHourAngle = dLocalMeanSiderealTime - dRightAscension;
			dLatitudeInRadians = dLatitude * pi / 180;
			dCos_Latitude = Math.cos(dLatitudeInRadians);
			dSin_Latitude = Math.sin(dLatitudeInRadians);
			dCos_HourAngle = Math.cos(dHourAngle);
			
			m_dZenithAngle = (Math.acos(dCos_Latitude * dCos_HourAngle * Math.cos(dDeclination) + Math.sin(dDeclination) * dSin_Latitude));
			dY = -Math.sin(dHourAngle);
			dX = Math.tan(dDeclination) * dCos_Latitude - dSin_Latitude * dCos_HourAngle;
			m_dAzimuth = Math.atan2(dY, dX);
			if (m_dAzimuth < 0.0)
				m_dAzimuth = m_dAzimuth + 2 * pi;
			m_dAzimuth = m_dAzimuth / rad;
			// Parallax Correction
			dParallax = (dEarthMeanRadius / dAstronomicalUnit) * Math.sin(m_dZenithAngle);
			m_dZenithAngle = (m_dZenithAngle + dParallax) / rad;
		}
	}
	
	// The following algorithm is adapted from: http://stackoverflow.com/questions/8708048/position-of-the-sun-given-time-of-day-latitude-and-longitude
	public void calculateSunPos2(int iYear, int iMonth, int iDay, double dHours, double dMins, double dSecs,
			  double dLong, double dLat)
	{
		//twopi <- 2 * pi
		//deg2rad <- pi / 180
		
		// Get day of the year, e.g. Feb 1 = 32, Mar 1 = 61 on leap years
		int[] numDaysMonth =  { 0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30 };
		
		for (int i=0; i<iMonth; i++)
			iDay += numDaysMonth[i];
		
		boolean leapDays = (iYear % 4 == 0) && (iYear % 400 == 0 | iYear % 100 != 0) && iDay >=60 && !(iMonth == 2 && iDay == 60);
		if (leapDays)
			iDay += 1;
		
		// Get Julian date - 2400000
		dHours = dHours + dMins / 60 + dSecs / 3600;
		int delta = iYear - 1949;
		int leap = (int)(delta / 4); // former leap years
		double julianDate = 32916.5 + delta * 365 + leap + iDay + dHours / 24;
		
		// The input to the Atronomer's almanach is the difference between
		// the Julian date and JD 2451545.0 (noon, 1 January 2000)
		double dTime = julianDate - 51545;
		
		// Ecliptic coordinates:
		
		// Mean longitude:
		double mnlong = 280.460 + .9856474 * dTime;
		mnlong = mnlong % 360;
		if (mnlong < 0)
			mnlong += 360;
		
		// Mean anomaly:
		double mnanom = 357.528 + .9856003 * dTime;
		mnanom = mnanom % 360;
		if (mnanom < 0)
			mnanom += 360;
		mnanom = mnanom * rad;
		
		// Ecliptic longitude and obliquity of ecliptic:
		double eclong = mnlong + 1.915 * Math.sin(mnanom) + 0.020 * Math.sin(2 * mnanom);
		eclong = eclong % 360;
		if (eclong < 0)
			eclong += 360;
		
		double oblqec = 23.439 - 0.0000004 * dTime;
		eclong = eclong * rad;
		oblqec = oblqec * rad;
		
		// Celestial coordinates:
		// Right ascension and declination:
		double num = Math.cos(oblqec) * Math.sin(eclong);
		double den = Math.cos(eclong);
		double ra = Math.atan(num / den);
		
		if (den < 0)
			ra += pi;
		
		if (den >= 0 && num < 0)
			ra += 2*pi;
		
		double dec = Math.asin(Math.sin(oblqec) * Math.sin(eclong));
		
		// Local coordinates:
		// Greenwich mean sidereal time:
		double gmst = 6.697375 + .0657098242 * dTime + dHours;
		gmst = gmst % 24;
		if (gmst < 0)
			gmst += 24;
		
		// Local mean sidereal time:
		double lmst = gmst + dLong / 15;
		lmst = lmst % 24;
		if (lmst < 0)
			lmst += 24;
		
		lmst = lmst * 15 * rad;
		
		// Hour angle
		double ha = lmst - ra;
		
		if (ha < -pi)
			ha += 2*pi;
		
		if (ha > pi)
			ha -=  2*pi;
		
		// Latitude to radians:
		dLat = dLat * rad;
		
		// Azimuth and elevation:
		double el = Math.asin(Math.sin(dec) * Math.sin(dLat) + Math.cos(dec) * Math.cos(dLat) * Math.cos(ha));
		double az = Math.asin(-Math.cos(dec) * Math.sin(ha) / Math.cos(el));
		
		// For logic and names, see Spencer, J.W. 1989. Solar Energy. 42(4):353
		boolean cosAzPos = (Math.sin(dec) - Math.sin(el) * Math.sin(dLat)) >= 0;
		boolean sinAzNeg = Math.sin(az) < 0;
		if (cosAzPos && sinAzNeg)
			az += 2*pi;
		if (!cosAzPos)
			az = pi - az;		
		
		m_dZenithAngle = el / rad;
		m_dAzimuth = az / rad;
	}
	
}
