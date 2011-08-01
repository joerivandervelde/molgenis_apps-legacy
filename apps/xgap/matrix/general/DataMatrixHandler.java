package matrix.general;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import matrix.AbstractDataMatrixInstance;
import matrix.implementations.binary.BinaryDataMatrixInstance;
import matrix.implementations.csv.CSVDataMatrixInstance;
import matrix.implementations.database.DatabaseDataMatrixInstance;

import org.molgenis.core.MolgenisFile;
import org.molgenis.data.Data;
import org.molgenis.data.DecimalDataElement;
import org.molgenis.data.TextDataElement;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.Query;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.util.Entity;
import org.molgenis.util.ValueLabel;

import app.DatabaseFactory;
import app.servlet.MolgenisServlet;
import decorators.MolgenisFileHandler;

/**
 * Class to handle the coupling of 'Data' objects and storage of the actual data
 * values
 * 
 * @author joerivandervelde
 * 
 */
public class DataMatrixHandler extends MolgenisFileHandler
{

	public DataMatrixHandler(Database db)
	{
		super(db);
		this.setVariantId(MolgenisServlet.getMolgenisVariantID());
	}

	/**
	 * Create a DataMatrix instance from this 'DataMatrix' object, regardless of
	 * its source.
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public AbstractDataMatrixInstance<Object> createInstance(Data data) throws Exception
	{
		AbstractDataMatrixInstance<Object> instance = null;
		File source = null;

		if (data.getStorage().equals("Database"))
		{
			instance = new DatabaseDataMatrixInstance(this.getDb(), data);
		}
		else
		{
			source = findSourceFile(data);
			if (source == null)
			{
				throw new FileNotFoundException("No MolgenisFile found referring to DataMatrix '" + data.getName()
						+ "'");
			}
			if (data.getStorage().equals("Binary"))
			{
				instance = new BinaryDataMatrixInstance(source);
			}
			if (data.getStorage().equals("CSV"))
			{
				instance = new CSVDataMatrixInstance(data, source);
			}
		}
		return instance;
	}

	/**
	 * Wrapper/helper function to attempt to remove a datamatrix. This means
	 * removing both the source and the 'Data' definition. WARNING: Removing
	 * 'Data' fails if other entities are still referring to this 'Data', but
	 * the source is already deleted by then. Only use if you are sure this
	 * 'Data' can be removed entirely. TODO: Somehow improve this behaviour.
	 * Make sure there are no other XREF's when considering datasource links,
	 * then remove source, then remove 'Data'.
	 * 
	 * @param dm
	 * @throws Exception
	 * @throws XGAPStorageException
	 */
	public void deleteDataMatrix(Data dm) throws Exception
	{
		try
		{
			deleteDataMatrixSource(dm);
		}
		catch (FileNotFoundException fnfe)
		{
			// no source found, continue to delete 'Data'
		}
		this.getDb().remove(dm);
	}

	/**
	 * Delete the source for this DataMatrix. Can be 'Database' or any kind of
	 * MolgenisFile subclass. (ie. 'Binary', 'CSV')
	 * 
	 * @param dm
	 * @throws Exception
	 * @throws XGAPStorageException
	 */
	public void deleteDataMatrixSource(Data dm) throws Exception
	{
		String verifiedSource = findSource(dm);

		if (verifiedSource.equals("null"))
		{
			throw new FileNotFoundException("No source to delete, appears to be null.");
		}
		else
		{
			if (verifiedSource.equals("Database"))
			{
				if (dm.getValueType().equals("Decimal"))
				{
					List<DecimalDataElement> dde = this.getDb().find(DecimalDataElement.class,
							new QueryRule("data", Operator.EQUALS, dm.getId()));
					this.getDb().remove(dde);
				}
				else
				{
					List<TextDataElement> tde = this.getDb().find(TextDataElement.class,
							new QueryRule("data", Operator.EQUALS, dm.getId()));
					this.getDb().remove(tde);
				}
			}
			else
			{
				this.getDb().remove(findMolgenisFile(dm));
			}
		}
	}

	/**
	 * Simple check to find out if this 'DataMatrix' has any kind of data source
	 * attached. (database, file, etc)
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 * @throws XGAPStorageException
	 */
	public boolean hasSource(Data data) throws Exception
	{
		if (data.getStorage().equals("Database"))
		{
			if (this.isDataMatrixStoredInDatabase(data))
			{
				return true;
			}
		}
		else
		{
			if (this.findSourceFile(data) != null)
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Enquire if this 'DataMatrix' is stored in a certain way. For example,
	 * 'Binary'.
	 * 
	 * @param data
	 * @param source
	 * @return
	 * @throws Exception
	 * @throws XGAPStorageException
	 */
	public boolean isDataStoredIn(Data data, String source) throws Exception
	{
		ArrayList<String> options = new ArrayList<String>();
		for (ValueLabel option : data.getStorageOptions())
		{
			options.add(option.getValue().toString());
		}

		if (!options.contains(source))
		{
			throw new DatabaseException("Invalid source type: " + source);
		}

		if (source.equals("Database"))
		{
			return isDataMatrixStoredInDatabase(data);
		}
		else
		{
			String matrixSource = source + "DataMatrix";
			// FIXME: pull out?
			List<? extends Entity> test = this.getDb().find(this.getDb().getClassForName(matrixSource));
			for (Entity e : test)
			{
				// used to be: if ((e.get("data_name").toString()).equals(data.getName()))
				if (new Integer(e.get("data").toString()).intValue() == data.getId().intValue())
				{
					try
					{
						this.getFile(e.get("name").toString());
						return true;
					}
					catch (FileNotFoundException fnf)
					{
						return false;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Helper-like function to find out if a 'DataMatrix' has values stored
	 * inside the database
	 * 
	 * @param data
	 * @return
	 * @throws DatabaseException
	 * @throws ParseException
	 */
	public boolean isDataMatrixStoredInDatabase(Data data) throws DatabaseException, ParseException
	{
		Query<DecimalDataElement> dde = this.getDb().query(DecimalDataElement.class);
		dde.limit(1);
		dde.equals("data", data.getId());
		Query<TextDataElement> tde = this.getDb().query(TextDataElement.class);
		tde.limit(1);
		tde.equals("data", data.getId());
		boolean hasDataElements = (dde.find().size() > 0 || tde.find().size() > 0) ? true : false;
		return hasDataElements;
	}

	/**
	 * Find a 'DataMatrix' object that possibly belongs as a field in this
	 * subtype of MolgenisFile
	 * 
	 * @param mf
	 * @return
	 * @throws DatabaseException
	 */
	public Data findData(MolgenisFile mf) throws DatabaseException
	{

		QueryRule mfName = new QueryRule("name", Operator.EQUALS, mf.getName());
		List<? extends Entity> mfToEntity = this.getDb().find(this.getDb().getClassForName(mf.get__Type()), mfName);

		if (mfToEntity.size() == 0)
		{
			throw new DatabaseException("No entities found for subclass '" + mf.get__Type() + "' and name '"
					+ mf.getName() + "'");
		}
		else if (mfToEntity.size() > 1)
		{
			throw new DatabaseException("SEVERE: Multiple entities found for subclass '" + mf.get__Type()
					+ "' and name '" + mf.getName() + "'");
		}
		String dataMatrixName = mfToEntity.get(0).get("dataMatrix_name").toString();
		QueryRule dataName = new QueryRule("name", Operator.EQUALS, dataMatrixName);
		List<Data> dataList = this.getDb().find(Data.class, dataName);

		if (dataList.size() == 0)
		{
			throw new DatabaseException("No matrix found for name '" + dataName + "'");
		}
		else if (dataList.size() > 1)
		{
			throw new DatabaseException("SEVERE: Multiple matrices found for name '" + dataName + "'");
		}
		else
		{
			return dataList.get(0);
		}

	}

	/**
	 * Find a 'MolgenisFile' object that possibly belongs to DataMatrix.
	 * 
	 * @param dm
	 * @return
	 * @throws DatabaseException
	 */
	public MolgenisFile findMolgenisFile(Data dm) throws DatabaseException
	{
		String matrixSource = dm.getStorage() + "DataMatrix";
		List<? extends Entity> test = this.getDb().find(this.getDb().getClassForName(matrixSource));
		for (Entity e : test)
		{
			if (Integer.valueOf(e.get("data").toString()).intValue() == dm.getId().intValue())
			{
				QueryRule mfId = new QueryRule("id", Operator.EQUALS, e.get(e.getIdField()));
				return this.getDb().find(MolgenisFile.class, mfId).get(0);
			}
		}
		return null;
	}

	/**
	 * Find the source file that belongs to this 'DataMatrix'. Returns null if
	 * not found or not applicable.
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 * @throws XGAPStorageException
	 */
	public File findSourceFile(Data data) throws Exception
	{
		List<? extends Entity> mfSubclasses = this.getDb().find(
				this.getDb().getClassForName(data.getStorage() + "DataMatrix"));
		for (Entity e : mfSubclasses)
		{
			// used to be: if ((e.get("data_name").toString()).equals(data.getName()))
			if (new Integer(e.get("data").toString()).intValue() == data.getId().intValue())
			{
				return this.getFile(e.get("name").toString());
			}

		}
		return null;
	}

	/**
	 * Iterate through all possible sources and return the source option (ie.
	 * 'Database', 'CSV') if there is a confirmed backend for this option.
	 * 
	 * @param data
	 * @return
	 * @throws Exception
	 * @throws XGAPStorageException
	 */
	public String findSource(Data data) throws Exception
	{
		ArrayList<String> options = new ArrayList<String>();
		for (ValueLabel option : data.getStorageOptions())
		{
			options.add(option.getValue().toString());
		}

		for (String option : options)
		{
			if (isDataStoredIn(data, option))
			{
				return option;
			}
		}
		return "null";
	}

	/**
	 * Example usage
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		Database db = DatabaseFactory.create("gcc.properties");
		DataMatrixHandler mh = new DataMatrixHandler(db);
		Data dm = db.find(Data.class).get(0);
		mh.isDataStoredIn(dm, "Binary");
	}

}
