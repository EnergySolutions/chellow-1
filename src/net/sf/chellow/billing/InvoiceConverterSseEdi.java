/*******************************************************************************
 * 
 *  Copyright (c) 2005, 2009 Wessex Water Services Limited
 *  
 *  This file is part of Chellow.
 * 
 *  Chellow is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Chellow is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Chellow.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *******************************************************************************/

package net.sf.chellow.billing;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.chellow.monad.Debug;
import net.sf.chellow.monad.Hiber;
import net.sf.chellow.monad.HttpException;
import net.sf.chellow.monad.InternalException;
import net.sf.chellow.monad.UserException;
import net.sf.chellow.physical.HhEndDate;
import net.sf.chellow.physical.MpanCore;
import net.sf.chellow.physical.ReadType;
import net.sf.chellow.physical.RegisterReadRaw;
import net.sf.chellow.physical.Units;

public class InvoiceConverterSseEdi implements InvoiceConverter {
	private static final Map<Integer, Character> readTypeMap = Collections
			.synchronizedMap(new HashMap<Integer, Character>());

	private static final Map<String, InvoiceType> invoiceTypeMap = Collections
			.synchronizedMap(new HashMap<String, InvoiceType>());

	private static final Map<String, Integer> tModMap = Collections
			.synchronizedMap(new HashMap<String, Integer>());

	static {
		readTypeMap.put(0, ReadType.TYPE_ROUTINE);
		readTypeMap.put(2, ReadType.TYPE_ESTIMATE);
		readTypeMap.put(4, ReadType.TYPE_CUSTOMER);
		readTypeMap.put(9, ReadType.TYPE_ROUTINE);

		invoiceTypeMap.put("A", InvoiceType.AMENDED);
		invoiceTypeMap.put("F", InvoiceType.FINAL);
		invoiceTypeMap.put("N", InvoiceType.NORMAL);
		invoiceTypeMap.put("I", InvoiceType.INTEREST);
		invoiceTypeMap.put("R", InvoiceType.RECONCILIATION);
		invoiceTypeMap.put("P", InvoiceType.PREPAID);
		invoiceTypeMap.put("O", InvoiceType.INFORMATION);
		invoiceTypeMap.put("W", InvoiceType.WITHDRAWAL);

		tModMap.put("URQ1", 1);
	}

	private LineNumberReader lreader;

	private List<InvoiceRaw> rawInvoices = new ArrayList<InvoiceRaw>();

	public InvoiceConverterSseEdi(Reader reader) throws HttpException,
			InternalException {
		lreader = new LineNumberReader(reader);
	}

	public List<InvoiceRaw> getRawInvoices() throws HttpException,
			InternalException {
		Hiber.flush();
		String line = null;
		try {
			line = lreader.readLine();
			Set<String> mpanStrings = null;
			DayStartDate issueDate = null;
			DayStartDate startDate = null;
			DayFinishDate finishDate = null;
			String accountReference = null;
			String invoiceNumber = null;
			BigDecimal net = null;
			BigDecimal vat = null;
			String messageType = null;
			Set<LocalRegisterReadRaw> reads = null;
			String invoiceTypeCode = null;

			while (line != null) {
				EdiSegment segment = null;
				if (line.endsWith("'")) {
					segment = new EdiSegment(line.substring(0,
							line.length() - 1), readTypeMap);
				} else {
					throw new UserException(
							"This parser expects one segment per line.");
				}
				String code = segment.getCode();
				if (code.equals("CLO")) {
					EdiElement cloc = segment.getElements().get(0);
					accountReference = cloc.getComponents().get(1);
				}
				if (code.equals("BCD")) {
					EdiElement ivdt = segment.getElements().get(0);
					EdiElement invn = segment.getElements().get(2);
					EdiElement btcd = segment.getElements().get(5);

					invoiceNumber = invn.getComponents().get(0);
					invoiceTypeCode = btcd.toString();
					issueDate = new DayStartDate(ivdt.getDate(0).getNext()
							.getDate());
				}
				if (code.equals("MHD")) {
					EdiElement type = segment.getElements().get(1);
					messageType = type.getComponents().get(0);
					// Debug.print("message type" + messageType);
					if (messageType.equals("UTLBIL")) {
						issueDate = null;
						startDate = null;
						finishDate = null;
						accountReference = null;
						invoiceNumber = null;
						net = new BigDecimal(0);
						vat = new BigDecimal(0);
						reads = new HashSet<LocalRegisterReadRaw>();
						invoiceTypeCode = null;
						mpanStrings = new HashSet<String>();
					}
				}
				if (code.equals("CCD")) {
					EdiElement ccde = segment.getElements().get(1);
					String consumptionChargeIndicator = ccde.getString(0);
					if (consumptionChargeIndicator.equals("1")
							|| consumptionChargeIndicator.equals("2")) {
						HhEndDate previousHhReadDate = segment.getElements()
								.get(7).getDate(0);
						DayStartDate registerStartDate = new DayStartDate(
								previousHhReadDate.getNext().getDate())
								.getNext();
						DayFinishDate previousReadDate = new DayFinishDate(
								previousHhReadDate).getNext();
						Debug.print("start date " + startDate);
						if (startDate == null
								|| startDate.getDate().after(
										registerStartDate.getDate())) {
							Debug.print("register date " + registerStartDate);
							startDate = registerStartDate;
						}
						DayFinishDate registerFinishDate = new DayFinishDate(
								segment.getElements().get(6).getDate(0))
								.getNext();
						if (finishDate == null
								|| finishDate.getDate().before(
										registerFinishDate.getDate())) {
							finishDate = registerFinishDate;
						}
						EdiElement tmod = segment.getElements().get(3);
						EdiElement mtnr = segment.getElements().get(4);
						EdiElement mloc = segment.getElements().get(5);
						EdiElement prrd = segment.getElements().get(9);
						EdiElement adjf = segment.getElements().get(12);
						ReadType presentReadType = prrd.getReadType(1);
						ReadType previousReadType = prrd.getReadType(3);
						BigDecimal coefficient = new BigDecimal(adjf.getInt(1))
								.divide(new BigDecimal(100000));
						BigDecimal presentReadingValue = new BigDecimal(prrd
								.getInt(0)).divide(new BigDecimal(1000));
						BigDecimal previousReadingValue = new BigDecimal(prrd
								.getInt(2)).divide(new BigDecimal(1000));
						String meterSerialNumber = mtnr.getString(0);
						MpanCore mpanCore = MpanCore.getMpanCore(mloc
								.getString(0).substring(0, 13));
						String tmodStr = tmod.getString(0);
						Integer tpr;
						try {
							tpr = Integer.parseInt(tmodStr);
						} catch (NumberFormatException e) {
							tpr = tModMap.get(tmodStr);
							if (tpr == null) {
								throw new UserException(
										"Don't recognize the TPR code '"
												+ tmodStr + "'.");
							}
						}
						reads.add(new LocalRegisterReadRaw(mpanCore,
								coefficient, meterSerialNumber, Units.KWH, tpr,
								true, previousReadDate, previousReadingValue,
								previousReadType, registerFinishDate,
								presentReadingValue, presentReadType));
					}
				}
				if (code.equals("MTR")) {
					if (messageType.equals("UTLBIL")) {
						Set<RegisterReadRaw> registerReads = new HashSet<RegisterReadRaw>();
						for (LocalRegisterReadRaw read : reads) {
							registerReads.add(new RegisterReadRaw(read
									.getMpanCore(), read.getCoefficient(), read
									.getMeterSerialNumber(), read.getUnits(),
									read.getTpr(), read.getPreviousDate(), read
											.getPreviousValue(), read
											.getPreviousType(), read
											.getCurrentDate(), read
											.getCurrentValue(), read
											.getCurrentType()));
						}
						InvoiceType invoiceType = invoiceTypeMap
								.get(invoiceTypeCode);
						InvoiceRaw invoiceRaw = new InvoiceRaw(invoiceType,
								accountReference, mpanStrings, invoiceNumber,
								issueDate, startDate, finishDate, net, vat,
								registerReads);
						rawInvoices.add(invoiceRaw);
					}
				}
				if (code.equals("MAN")) {
					EdiElement madn = segment.getElements().get(2);
					String pcCode = "0" + madn.getString(3);
					String mtcCode = madn.getComponents().get(4);
					String llfcCode = madn.getString(5);

					mpanStrings.add(pcCode + " " + mtcCode + " " + llfcCode
							+ " " + madn.getComponents().get(0) + " "
							+ madn.getComponents().get(1)
							+ madn.getComponents().get(2));
				}
				if (code.equals("VAT")) {
					EdiElement uvla = segment.getElements().get(5);
					net = uvla.getBigDecimal();
					EdiElement uvtt = segment.getElements().get(6);
					vat = uvtt.getBigDecimal();
				}
				line = lreader.readLine();
			}
			lreader.close();
			Hiber.flush();
		} catch (IOException e) {
			throw new UserException("Can't read EDF Energy mm file.");
		} catch (Throwable e) {
			throw new UserException("Problem at line number "
					+ lreader.getLineNumber() + "\n" + line
					+ "\n of the EDI file.\n"
					+ HttpException.getStackTraceString(e));
		}
		return rawInvoices;
	}

	public String getProgress() {
		return "Reached line " + lreader.getLineNumber() + ".";
	}

	private class LocalRegisterReadRaw {
		private MpanCore mpanCore;

		private BigDecimal coefficient;

		private String meterSerialNumber;

		private Units units;

		private int tpr;

		private boolean isImport;

		private DayFinishDate previousDate;

		private BigDecimal previousValue;

		private ReadType previousType;

		private DayFinishDate currentDate;

		private BigDecimal currentValue;

		private ReadType currentType;

		public LocalRegisterReadRaw(MpanCore mpanCore, BigDecimal coefficient,
				String meterSerialNumber, Units units, Integer tpr,
				boolean isImport, DayFinishDate previousDate,
				BigDecimal previousValue, ReadType previousType,
				DayFinishDate currentDate, BigDecimal currentValue,
				ReadType currentType) throws InternalException {
			this.mpanCore = mpanCore;
			this.coefficient = coefficient;
			this.meterSerialNumber = meterSerialNumber;
			this.units = units;
			if (tpr == null) {
				throw new InternalException("TPR cannot be null.");
			}
			this.tpr = tpr;
			this.isImport = isImport;
			this.previousDate = previousDate;
			this.previousValue = previousValue;
			this.previousType = previousType;
			this.currentDate = currentDate;
			this.currentValue = currentValue;
			this.currentType = currentType;
		}

		public MpanCore getMpanCore() {
			return mpanCore;
		}

		public BigDecimal getCoefficient() {
			return coefficient;
		}

		public String getMeterSerialNumber() {
			return meterSerialNumber;
		}

		public Units getUnits() {
			return units;
		}

		public int getTpr() {
			return tpr;
		}

		public boolean getIsImport() {
			return isImport;
		}

		public DayFinishDate getPreviousDate() {
			return previousDate;
		}

		public BigDecimal getPreviousValue() {
			return previousValue;
		}

		public ReadType getPreviousType() {
			return previousType;
		}

		public DayFinishDate getCurrentDate() {
			return currentDate;
		}

		public BigDecimal getCurrentValue() {
			return currentValue;
		}

		public ReadType getCurrentType() {
			return currentType;
		}

	}
}