package org.janelia.stitching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.janelia.util.ComparableTuple;
import org.janelia.util.Conversions;

import net.imglib2.Interval;
import net.imglib2.util.Intervals;

public class CheckSubTileConfigurationCoplanarity
{
	public static TreeMap< ComparableTuple< Long >, Integer > groupSubTilesByTheirLocalPosition( final Collection< ? extends Interval > subTileIntervals )
	{
		final TreeMap< ComparableTuple< Long >, Integer > groups = new TreeMap<>();
		for ( final Interval subTileInterval : subTileIntervals )
		{
			final ComparableTuple< Long > key = new ComparableTuple<>( Conversions.toBoxedArray( Intervals.minAsLongArray( subTileInterval ) ) );
			groups.put( key, groups.getOrDefault( key, 0 ) + 1 );
		}
		return groups;
	}

	public static boolean checkCoplanarity( final TreeMap< ComparableTuple< Long >, Integer > groupedSubTileIntervals )
	{
		if ( !groupedSubTileIntervals.isEmpty() && groupedSubTileIntervals.firstKey().length != 3 )
			throw new IllegalArgumentException( "incorrect dimensionality: " + groupedSubTileIntervals.firstKey().length );

		if ( groupedSubTileIntervals.size() < 4 )
			return true;

		if ( groupedSubTileIntervals.size() > 4 )
			return false;

		// Check two possible coplanar configurations:

		// 1) all 4 points have the same coordinate in any dimension (the plane is orthogonal to the tile)
		for ( int d = 0; d < 3; ++d )
		{
			final TreeSet< Long > positionInDimension = new TreeSet<>();
			for ( final ComparableTuple< Long > group : groupedSubTileIntervals.keySet() )
				positionInDimension.add( group.getValue( d ) );
			if ( positionInDimension.size() <= 1 )
				return true;
		}

		// or, 2) there are 2 subgroups in 2 dimensions having the same coordinates (the plane is tilted 45 degrees)
A:		for ( int d = 0; d < 3; ++d )
		{
			final TreeMap< ComparableTuple< Long >, Integer > subgroups = new TreeMap<>();
			for ( final ComparableTuple< Long > group : groupedSubTileIntervals.keySet() )
			{
				final List< Integer > subgroupDimensions = new ArrayList<>();
				for ( int k = 0; k < 3; ++k )
					if ( k != d )
						subgroupDimensions.add( k );

				final ComparableTuple< Long > subgroup = new ComparableTuple<>( group.getValue( subgroupDimensions.get( 0 ) ), group.getValue( subgroupDimensions.get( 1 ) ) );
				if ( subgroup.getValue( 0 ) != subgroup.getValue( 1 ) )
					continue A;

				subgroups.put( subgroup, subgroups.getOrDefault( subgroup, 0 ) + 1 );
			}

			if ( subgroups.size() == 2 )
			{
				if ( subgroups.firstEntry().getValue() == 2 && subgroups.lastEntry().getValue() == 2 )
					return true;
				else
					throw new RuntimeException( "unexpected: " + subgroups );
			}
		}

		return false;
	}
}
