package org.janelia.stitching.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.janelia.stitching.Boundaries;
import org.janelia.stitching.SerializablePairWiseStitchingResult;
import org.janelia.stitching.TileInfoJSONProvider;
import org.janelia.stitching.TileOperations;
import org.janelia.stitching.TilePair;
import org.janelia.stitching.Utils;

/**
 * @author Igor Pisarev
 */

public class FilterAdjacentShiftsMulti
{
	public static void main( final String[] args ) throws Exception
	{
		final List< SerializablePairWiseStitchingResult[] > shifts = TileInfoJSONProvider.loadPairwiseShiftsMulti( args[ 0 ] );
		final String dimStr = args.length > 1 ? args[ 1 ] : "";
		if ( !dimStr.isEmpty() && !dimStr.equals( "x" ) && !dimStr.equals("y") && !dimStr.equals( "z" ) )
			throw new Exception( "Unknown value" );

		final String coordsPatternStr = ".*(\\d{3})x_(\\d{3})y_(\\d{3})z.*";
		final Pattern coordsPattern = Pattern.compile( coordsPatternStr );
		final String filename = null;
		final Matcher matcher = null;

		final List< SerializablePairWiseStitchingResult[] > shiftsCopy = new ArrayList<>( shifts );

		final List< SerializablePairWiseStitchingResult[] > adjacentShifts = new ArrayList<>();

		int validShifts = 0;
		/*for ( final SerializablePairWiseStitchingResult shift : shifts )
		{
			if ( !shift.getIsValidOverlap() )
				continue;


			if ( shift.getTilePair().first().getIndex() == 210 || shift.getTilePair().second().getIndex() == 210 )
				continue;


			validShifts++;
			shift.setIsValidOverlap( false );
			filename = Paths.get( shift.getTilePair().first().getFilePath() ).getFileName().toString();
			matcher = coordsPattern.matcher( filename );
			if ( !matcher.find() )
				throw new Exception( "Can't parse coordinates string" );
			final int[] coordsArr1 = new int[] { Integer.parseInt( matcher.group( 1 ) ), Integer.parseInt( matcher.group( 2 ) ), Integer.parseInt( matcher.group( 3 ) ) };

			filename = Paths.get( shift.getTilePair().second().getFilePath() ).getFileName().toString();
			matcher = coordsPattern.matcher( filename );
			if ( !matcher.find() )
				throw new Exception( "Can't parse coordinates string" );
			final int[] coordsArr2 = new int[] { Integer.parseInt( matcher.group( 1 ) ), Integer.parseInt( matcher.group( 2 ) ), Integer.parseInt( matcher.group( 3 ) ) };

			int diffs = 0, singleDiffAbs = 0, singleDiffDim = 0;
			for ( int d = 0; d < coordsArr1.length; d++ )
			{
				if ( coordsArr1[ d ] != coordsArr2[ d ] )
				{
					diffs++;
					singleDiffAbs = Math.abs( coordsArr1[ d ] - coordsArr2[ d ] );
					singleDiffDim = d;
				}
			}
			if ( diffs == 0 || singleDiffAbs == 0 )
				throw new Exception( "Impossible" );
			if ( diffs > 1 )
				continue;

			if ( singleDiffAbs > 1 )
				throw new Exception( "Too far away" );

			// this is an adjacent shift
			if ( shift.getTilePair().first().getPosition( singleDiffDim ) > shift.getTilePair().second().getPosition( singleDiffDim ) )
				shift.swap();
			adjacentShifts.add( shift );
			shift.setIsValidOverlap( true );

			if ( shift.getTilePair().first().getIndex() == 210 || shift.getTilePair().second().getIndex() == 210 )
				System.out.println( "this is an adjacent shift (" + shift.getTilePair().first().getIndex() + "," + shift.getTilePair().second().getIndex() + ")" );
		}*/



		final Map< Integer, Set< Integer> > validation = new HashMap<>();
		int validationCount = 0, swappedCount = 0;
		A:		for ( final SerializablePairWiseStitchingResult[] shift : shiftsCopy )
		{
			for (int peak=0; peak<shift.length; peak++ )
				if ( !shift[peak].getIsValidOverlap() )
					continue A;

			validShifts++;
			for (int peak=0; peak<shift.length; peak++ )
				shift[peak].setIsValidOverlap( false );

			final Boundaries overlap = TileOperations.getOverlappingRegionGlobal( shift[0].getTilePair().first(), shift[0].getTilePair().second() );

			final boolean[] shortEdges = new boolean[overlap.numDimensions() ];
			for ( int d = 0; d < overlap.numDimensions(); d++ )
			{
				final int maxPossibleOverlap = ( int ) Math.min( shift[0].getTilePair().first().getSize( d ), shift[0].getTilePair().second().getSize( d ) );
				if ( overlap.dimension( d ) < maxPossibleOverlap / 2 )
					shortEdges[d] = true;
			}

			if (
					( ( !shortEdges[0] || shortEdges[1] || shortEdges[2] )  ||  (!dimStr.isEmpty() && !dimStr.equals( "x" )) )   && // x
					( ( shortEdges[0] || !shortEdges[1] || shortEdges[2] )  ||  (!dimStr.isEmpty() && !dimStr.equals( "y" )) )   && // y
					( ( shortEdges[0] || shortEdges[1] || !shortEdges[2] )  ||  (!dimStr.isEmpty() && !dimStr.equals( "z" )) )   && // z

					true
					)
				continue;

			final int ind1 = Math.min( shift[0].getTilePair().first().getIndex(), shift[0].getTilePair().second().getIndex() );
			final int ind2 = Math.max( shift[0].getTilePair().first().getIndex(), shift[0].getTilePair().second().getIndex() );
			if ( !validation.containsKey( ind1 ) )
				validation.put( ind1, new HashSet<>() );
			validation.get( ind1 ).add( ind2 );
			validationCount++;

			// swap the pair of tiles if needed
			for ( int d = 0; d < shortEdges.length; d++ )
			{
				if ( shortEdges[ d ] )
				{
					if ( shift[0].getTilePair().first().getPosition( d ) > shift[0].getTilePair().second().getPosition( d ) )
					{
						swappedCount++;
						for (int peak=0; peak<shift.length; peak++ )
							shift[peak].swap();
					}
					break;
				}
			}

			// just for Sample9
			for (int peak=0; peak<shift.length; peak++ )
				shift[peak].setIsValidOverlap( true );
			adjacentShifts.add( shift );
		}

		System.out.println( "Swapped " + swappedCount + " pairs" );

		/*if ( validationCount != adjacentShifts.size() )
			throw new Exception( "Validation set size=" + validationCount + ", adj=" + adjacentShifts.size() );
		for ( final SerializablePairWiseStitchingResult adjShift : adjacentShifts )
		{
			final int ind1 = Math.min( adjShift.getTilePair().first().getIndex(), adjShift.getTilePair().second().getIndex() );
			final int ind2 = Math.max( adjShift.getTilePair().first().getIndex(), adjShift.getTilePair().second().getIndex() );
			if ( !( validation.containsKey( ind1) && validation.get( ind1 ).contains( ind2 ) ) )
				throw new Exception( "Something is missing from the validation set" );
		}
		System.out.println( "*** Both methods resulted in the same set of adjacent shifts ***" );*/


		System.out.println( "There are " + adjacentShifts.size() + " adjacent shifts of " + validShifts );
		TileInfoJSONProvider.savePairwiseShiftsMulti( shifts, Utils.addFilenameSuffix( args[ 0 ], "_adj" + (!dimStr.isEmpty()?"-"+dimStr:"" ) ) );

		System.out.println( "Done" );
	}



	public static List< TilePair > filterAdjacentPairs( final List< TilePair > overlappingPairs, final int dim )
	{
		return filterAdjacentPairs( overlappingPairs, new Integer(dim) );
	}

	public static List< TilePair > filterAdjacentPairs( final List< TilePair > overlappingPairs )
	{
		return filterAdjacentPairs( overlappingPairs, null );
	}

	private static List< TilePair > filterAdjacentPairs( final List< TilePair > overlappingPairs, final Integer dim )
	{
		final List< TilePair > adjacentPairs = new ArrayList<>();

		for ( final TilePair pair : overlappingPairs )
		{
			final Boundaries overlap = TileOperations.getOverlappingRegionGlobal( pair.first(), pair.second() );

			final boolean[] shortEdges = new boolean[overlap.numDimensions() ];
			for ( int d = 0; d < overlap.numDimensions(); d++ )
			{
				final int maxPossibleOverlap = ( int ) Math.min( pair.first().getSize( d ), pair.second().getSize( d ) );
				if ( overlap.dimension( d ) < maxPossibleOverlap / 2 )
					shortEdges[d] = true;
			}

			if (
					( ( !shortEdges[0] || shortEdges[1] || shortEdges[2] )  ||  (dim != null && dim.intValue() != 0)  ) && // x
					( ( shortEdges[0] || !shortEdges[1] || shortEdges[2] )  ||  (dim != null && dim.intValue() != 1)  ) && // y
					( ( shortEdges[0] || shortEdges[1] || !shortEdges[2] )  ||  (dim != null && dim.intValue() != 2)  ) && // z

					true
					)
				continue;

			adjacentPairs.add( pair );
		}

		return adjacentPairs;
	}
}
