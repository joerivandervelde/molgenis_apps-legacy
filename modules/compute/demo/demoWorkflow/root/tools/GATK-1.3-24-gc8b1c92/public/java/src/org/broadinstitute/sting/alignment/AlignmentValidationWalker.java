/*
 * Copyright (c) 2010 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broadinstitute.sting.alignment;

import org.broadinstitute.sting.alignment.bwa.BWAConfiguration;
import org.broadinstitute.sting.alignment.bwa.BWTFiles;
import org.broadinstitute.sting.alignment.bwa.c.BWACAligner;
import org.broadinstitute.sting.commandline.Argument;
import org.broadinstitute.sting.gatk.contexts.ReferenceContext;
import org.broadinstitute.sting.gatk.refdata.ReadMetaDataTracker;
import org.broadinstitute.sting.gatk.walkers.ReadWalker;
import org.broadinstitute.sting.utils.BaseUtils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.sam.GATKSAMRecord;

import java.util.Iterator;

/**
 * Validates consistency of the aligner interface by taking reads already aligned by BWA in a BAM file, stripping them
 * of their alignment data, realigning them, and making sure one of the best resulting realignments matches the original
 * alignment from the input file.
 *
 * @author mhanna
 * @version 0.1
 */
public class AlignmentValidationWalker extends ReadWalker<Integer,Integer> {
    /**
     * The supporting BWT index generated using BWT. 
     */
    @Argument(fullName="BWTPrefix",shortName="BWT",doc="Index files generated by bwa index -d bwtsw",required=false)
    private String prefix = null;

    /**
     * The instance used to generate alignments.
     */
    private BWACAligner aligner = null;

    /**
     * Create an aligner object.  The aligner object will load and hold the BWT until close() is called.
     */
    @Override
    public void initialize() {
        if(prefix == null)
            prefix = getToolkit().getArguments().referenceFile.getAbsolutePath();
        BWTFiles bwtFiles = new BWTFiles(prefix);
        BWAConfiguration configuration = new BWAConfiguration();
        aligner = new BWACAligner(bwtFiles,configuration);
    }

    /**
     * Aligns a read to the given reference.
     *
     * @param ref Reference over the read.  Read will most likely be unmapped, so ref will be null.
     * @param read Read to align.
     * @return Number of reads aligned by this map (aka 1).
     */
    @Override
    public Integer map(ReferenceContext ref, GATKSAMRecord read, ReadMetaDataTracker metaDataTracker) {
        //logger.info(String.format("examining read %s", read.getReadName()));

        byte[] bases = read.getReadBases();
        if(read.getReadNegativeStrandFlag()) bases = BaseUtils.simpleReverseComplement(bases);

        boolean matches = true;
        Iterable<Alignment[]> alignments = aligner.getAllAlignments(bases);
        Iterator<Alignment[]> alignmentIterator = alignments.iterator();

        if(!alignmentIterator.hasNext()) {
            matches = read.getReadUnmappedFlag();
        }
        else {
            Alignment[] alignmentsOfBestQuality = alignmentIterator.next();
            for(Alignment alignment: alignmentsOfBestQuality) {
                matches = (alignment.getContigIndex() == read.getReferenceIndex());
                matches &= (alignment.getAlignmentStart() == read.getAlignmentStart());
                matches &= (alignment.isNegativeStrand() == read.getReadNegativeStrandFlag());
                matches &= (alignment.getCigar().equals(read.getCigar()));
                matches &= (alignment.getMappingQuality() == read.getMappingQuality());
                if(matches) break;
            }
        }

        if(!matches) {
            logger.error("Found mismatch!");
            logger.error(String.format("Read %s:",read.getReadName()));
            logger.error(String.format("    Contig index: %d",read.getReferenceIndex()));
            logger.error(String.format("    Alignment start: %d", read.getAlignmentStart()));
            logger.error(String.format("    Negative strand: %b", read.getReadNegativeStrandFlag()));
            logger.error(String.format("    Cigar: %s%n", read.getCigarString()));
            logger.error(String.format("    Mapping quality: %s%n", read.getMappingQuality()));
            for(Alignment[] alignmentsByScore: alignments) {
                for(int i = 0; i < alignmentsByScore.length; i++) {
                    logger.error(String.format("Alignment %d:",i));
                    logger.error(String.format("    Contig index: %d",alignmentsByScore[i].getContigIndex()));
                    logger.error(String.format("    Alignment start: %d", alignmentsByScore[i].getAlignmentStart()));
                    logger.error(String.format("    Negative strand: %b", alignmentsByScore[i].isNegativeStrand()));
                    logger.error(String.format("    Cigar: %s", alignmentsByScore[i].getCigarString()));
                    logger.error(String.format("    Mapping quality: %s%n", alignmentsByScore[i].getMappingQuality()));
                }
            }
            throw new ReviewedStingException(String.format("Read %s mismatches!", read.getReadName()));
        }

        return 1;
    }

    /**
     * Initial value for reduce.  In this case, validated reads will be counted.
     * @return 0, indicating no reads yet validated.
     */
    @Override
    public Integer reduceInit() { return 0; }

    /**
     * Calculates the number of reads processed.
     * @param value Number of reads processed by this map.
     * @param sum Number of reads processed before this map.
     * @return Number of reads processed up to and including this map.
     */
    @Override
    public Integer reduce(Integer value, Integer sum) {
        return value + sum;
    }

    /**
     * Cleanup.
     * @param result Number of reads processed.
     */
    @Override
    public void onTraversalDone(Integer result) {
        aligner.close();
        super.onTraversalDone(result);
    }

}
