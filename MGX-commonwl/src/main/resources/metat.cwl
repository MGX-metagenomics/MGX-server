$graph:
- class: Workflow
  id: '#main'
  inputs:
  - doc: MGX seqrun IDs
    https://www.sevenbridges.com/x: -960.977783203125
    https://www.sevenbridges.com/y: -519.2344360351562
    id: '#main/runIds'
    type:
      items: string
      type: array
  - https://www.sevenbridges.com/x: -959.557861328125
    https://www.sevenbridges.com/y: -141.82296752929688
    id: '#main/apiKey'
    type: string
  - https://www.sevenbridges.com/x: -959.6152954101562
    https://www.sevenbridges.com/y: -395.3645935058594
    id: '#main/projectName'
    type: string
  - https://www.sevenbridges.com/x: -955.8181762695312
    https://www.sevenbridges.com/y: -272.5052490234375
    id: '#main/hostURI'
    type: string
  - https://www.sevenbridges.com/x: 4488.1689453125
    https://www.sevenbridges.com/y: -85.73023986816406
    id: '#main/assemblyName'
    type: string
  - https://www.sevenbridges.com/x: -185.31167602539062
    https://www.sevenbridges.com/y: 196.1172637939453
    id: '#main/sequencingAdaptersFile'
    type: File
  label: Metatranscriptome Assembly and Quantification
  outputs:
  - https://www.sevenbridges.com/x: 4874.22802734375
    https://www.sevenbridges.com/y: -308.88360595703125
    id: '#main/success'
    outputSource:
    - '#main/annotationclient/success'
    type: boolean
  requirements:
  - class: ScatterFeatureRequirement
  - class: MultipleInputFeatureRequirement
  - class: InlineJavascriptRequirement
  steps:
  - https://www.sevenbridges.com/x: -674.2255859375
    https://www.sevenbridges.com/y: 115.66755676269531
    id: '#main/seqrunfetch'
    in:
    - id: '#main/seqrunfetch/apiKey'
      source: '#main/apiKey'
    - id: '#main/seqrunfetch/hostURI'
      source: '#main/hostURI'
    - id: '#main/seqrunfetch/projectName'
      source: '#main/projectName'
    - id: '#main/seqrunfetch/runId'
      source: '#main/runIds'
    label: MGX Fetch sequences
    out:
    - id: '#main/seqrunfetch/fwdReads'
    - id: '#main/seqrunfetch/revReads'
    - id: '#main/seqrunfetch/singleReads'
    run: '#seqrunfetch.cwl'
    scatter:
    - '#main/seqrunfetch/runId'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 444.4377136230469
    https://www.sevenbridges.com/y: 241.66441345214844
    id: '#main/rnaspades'
    in:
    - id: '#main/rnaspades/read1'
      source:
      - '#main/trimmomatic_pe/fwdTrimmed'
    - id: '#main/rnaspades/read2'
      source:
      - '#main/trimmomatic_pe/revTrimmed'
    - default: 125
      id: '#main/rnaspades/thread-number'
    - id: '#main/rnaspades/unpaired'
      source:
      - '#main/trimmomatic_se/fwdTrimmed'
    label: rnaSPAdes
    out:
    - id: '#main/rnaspades/contigs'
    run: '#rnaspades.cwl'
  - https://www.sevenbridges.com/x: 2489.327880859375
    https://www.sevenbridges.com/y: 239.57028198242188
    id: '#main/prodigal'
    in:
    - id: '#main/prodigal/inputFile'
      source: '#main/rnaspades/contigs'
    - default: true
      id: '#main/prodigal/metagenomic'
    label: Prodigal 2.6.3
    out:
    - id: '#main/prodigal/annotations'
    - id: '#main/prodigal/genes'
    - id: '#main/prodigal/proteins'
    run: '#prodigal.cwl'
  - https://www.sevenbridges.com/x: 1672.1806640625
    https://www.sevenbridges.com/y: 894.350830078125
    id: '#main/samtools_sam2bam'
    in:
    - id: '#main/samtools_sam2bam/input'
      source: '#main/strobealign_pe/sam'
    out:
    - id: '#main/samtools_sam2bam/output'
    run: '#samtools-sam2bam.cwl'
    scatter:
    - '#main/samtools_sam2bam/input'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1964.7889404296875
    https://www.sevenbridges.com/y: 893.903564453125
    id: '#main/samtools_sort'
    in:
    - id: '#main/samtools_sort/input'
      source: '#main/samtools_sam2bam/output'
    out:
    - id: '#main/samtools_sort/output'
    run: '#samtools-sort.cwl'
    scatter:
    - '#main/samtools_sort/input'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 3332.6796875
    https://www.sevenbridges.com/y: 333.0718994140625
    id: '#main/feature_counts_se'
    in:
    - id: '#main/feature_counts_se/annotation'
      source: '#main/prodigal/annotations'
    - id: '#main/feature_counts_se/bamFile'
      source: '#main/samtools_sort_1/output'
    out:
    - id: '#main/feature_counts_se/output_counts'
    run: '#featureCounts.cwl'
    scatter:
    - '#main/feature_counts_se/bamFile'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 2846.90625
    https://www.sevenbridges.com/y: 1069.1043701171875
    id: '#main/samtools_merge_all'
    in:
    - id: '#main/samtools_merge_all/inputs'
      linkMerge: merge_flattened
      source:
      - '#main/samtools_sort/output'
      - '#main/samtools_sort_1/output'
    out:
    - id: '#main/samtools_merge_all/output'
    run: '#samtools-merge.cwl'
  - https://www.sevenbridges.com/x: 3347.258056640625
    https://www.sevenbridges.com/y: 562.1612548828125
    id: '#main/feature_counts_pe'
    in:
    - id: '#main/feature_counts_pe/annotation'
      source: '#main/prodigal/annotations'
    - id: '#main/feature_counts_pe/bamFile'
      source: '#main/samtools_sort/output'
    out:
    - id: '#main/feature_counts_pe/output_counts'
    run: '#featureCounts.cwl'
    scatter:
    - '#main/feature_counts_pe/bamFile'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 4642.23876953125
    https://www.sevenbridges.com/y: -306
    id: '#main/annotationclient'
    in:
    - id: '#main/annotationclient/apiKey'
      source: '#main/apiKey'
    - id: '#main/annotationclient/assemblyName'
      source: '#main/assemblyName'
    - id: '#main/annotationclient/binnedFastas'
      linkMerge: merge_nested
      source:
      - '#main/rnaspades/contigs'
    - id: '#main/annotationclient/contigCoverage'
      source: '#main/bamstats/tsvOutput'
    - id: '#main/annotationclient/featureCountsPerSample'
      linkMerge: merge_flattened
      source:
      - '#main/feature_counts_se/output_counts'
      - '#main/feature_counts_pe/output_counts'
    - id: '#main/annotationclient/featureCountsTotal'
      source: '#main/merge_f_c/tsvOutput'
    - id: '#main/annotationclient/hostURI'
      source: '#main/hostURI'
    - id: '#main/annotationclient/predictedGenes'
      source: '#main/prodigal/annotations'
    - id: '#main/annotationclient/projectName'
      source: '#main/projectName'
    - id: '#main/annotationclient/runIds'
      source:
      - '#main/runIds'
    label: MGX Annotate
    out:
    - id: '#main/annotationclient/success'
    run: '#annotationclient.cwl'
  - https://www.sevenbridges.com/x: 3370.869140625
    https://www.sevenbridges.com/y: 1072.1676025390625
    id: '#main/bamstats'
    in:
    - id: '#main/bamstats/bamFile'
      source: '#main/samtools_merge_all/output'
    - default: contig_coverage.tsv
      id: '#main/bamstats/outFile'
    label: 'bamstats: BAM alignment statistics per reference sequence'
    out:
    - id: '#main/bamstats/tsvOutput'
    run: '#bamstats.cwl'
  - https://www.sevenbridges.com/x: 1346.361572265625
    https://www.sevenbridges.com/y: 897.0208129882812
    id: '#main/strobealign_pe'
    in:
    - id: '#main/strobealign_pe/read1'
      source: '#main/trimmomatic_pe/fwdTrimmed'
    - id: '#main/strobealign_pe/read2'
      source: '#main/trimmomatic_pe/revTrimmed'
    - id: '#main/strobealign_pe/reference'
      source: '#main/rnaspades/contigs'
    label: 'StrobeAlign: map PE reads'
    out:
    - id: '#main/strobealign_pe/sam'
    run: '#strobealign.cwl'
    scatter:
    - '#main/strobealign_pe/read1'
    - '#main/strobealign_pe/read2'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 121.0569839477539
    https://www.sevenbridges.com/y: 76.77188873291016
    id: '#main/trimmomatic_se'
    in:
    - default: SE
      id: '#main/trimmomatic_se/end_mode'
    - id: '#main/trimmomatic_se/fwdReads'
      source: '#main/ribodetector/fwdFiltered'
    - id: '#main/trimmomatic_se/input_adapters_file'
      source: '#main/sequencingAdaptersFile'
    out:
    - id: '#main/trimmomatic_se/fwdTrimmed'
    - id: '#main/trimmomatic_se/revTrimmed'
    run: '#trimmomatic.cwl'
    scatter:
    - '#main/trimmomatic_se/fwdReads'
    scatterMethod: dotproduct
    when: $(inputs.fwdReads != null)
  - https://www.sevenbridges.com/x: 117.1124038696289
    https://www.sevenbridges.com/y: 341.240966796875
    id: '#main/trimmomatic_pe'
    in:
    - default: PE
      id: '#main/trimmomatic_pe/end_mode'
    - id: '#main/trimmomatic_pe/fwdReads'
      source: '#main/ribodetector_1/fwdFiltered'
    - id: '#main/trimmomatic_pe/revReads'
      source: '#main/ribodetector_1/revFiltered'
    - id: '#main/trimmomatic_pe/input_adapters_file'
      source: '#main/sequencingAdaptersFile'
    out:
    - id: '#main/trimmomatic_pe/fwdTrimmed'
    - id: '#main/trimmomatic_pe/revTrimmed'
    run: '#trimmomatic.cwl'
    scatter:
    - '#main/trimmomatic_pe/fwdReads'
    - '#main/trimmomatic_pe/revReads'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1324.580810546875
    https://www.sevenbridges.com/y: 50.46404266357422
    id: '#main/strobealign_se'
    in:
    - id: '#main/strobealign_se/read1'
      source: '#main/trimmomatic_se/fwdTrimmed'
    - id: '#main/strobealign_se/reference'
      source: '#main/rnaspades/contigs'
    label: 'StrobeAlign: map SE reads'
    out:
    - id: '#main/strobealign_se/sam'
    run: '#strobealign.cwl'
    scatter:
    - '#main/strobealign_se/read1'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1661.573974609375
    https://www.sevenbridges.com/y: 56.39213180541992
    id: '#main/samtools_sam2bam_1'
    in:
    - id: '#main/samtools_sam2bam_1/input'
      source: '#main/strobealign_se/sam'
    out:
    - id: '#main/samtools_sam2bam_1/output'
    run: '#samtools-sam2bam.cwl'
    scatter:
    - '#main/samtools_sam2bam_1/input'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 2009.41796875
    https://www.sevenbridges.com/y: 51
    id: '#main/samtools_sort_1'
    in:
    - id: '#main/samtools_sort_1/input'
      source: '#main/samtools_sam2bam_1/output'
    out:
    - id: '#main/samtools_sort_1/output'
    run: '#samtools-sort.cwl'
    scatter:
    - '#main/samtools_sort_1/input'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 3665.084228515625
    https://www.sevenbridges.com/y: 403.438232421875
    id: '#main/merge_f_c'
    in:
    - id: '#main/merge_f_c/featureCountsTSV'
      linkMerge: merge_flattened
      source:
      - '#main/feature_counts_se/output_counts'
      - '#main/feature_counts_pe/output_counts'
    - default: genecoverage_total.tsv
      id: '#main/merge_f_c/outFile'
    out:
    - id: '#main/merge_f_c/tsvOutput'
    run: '#mergeFC.cwl'
  - https://www.sevenbridges.com/x: -461.6506042480469
    https://www.sevenbridges.com/y: 42.87717056274414
    id: '#main/ribodetector'
    in:
    - id: '#main/ribodetector/fwdReads'
      source: '#main/seqrunfetch/singleReads'
    label: RiboDetector
    out:
    - id: '#main/ribodetector/fwdFiltered'
    - id: '#main/ribodetector/revFiltered'
    run: '#ribodetector.cwl'
    scatter:
    - '#main/ribodetector/fwdReads'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: -459.84661865234375
    https://www.sevenbridges.com/y: 350.7164611816406
    id: '#main/ribodetector_1'
    in:
    - id: '#main/ribodetector_1/fwdReads'
      source: '#main/seqrunfetch/fwdReads'
    - id: '#main/ribodetector_1/revReads'
      source: '#main/seqrunfetch/revReads'
    label: RiboDetector
    out:
    - id: '#main/ribodetector_1/fwdFiltered'
    - id: '#main/ribodetector_1/revFiltered'
    run: '#ribodetector.cwl'
    scatter:
    - '#main/ribodetector_1/fwdReads'
    - '#main/ribodetector_1/revReads'
    scatterMethod: dotproduct
- arguments:
  - position: 5
    prefix: -d
    valueFrom: $(runtime.outdir)
  baseCommand:
  - AnnotationClient
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/mgxannotate
  id: '#annotationclient.cwl'
  inputs:
  - id: '#annotationclient.cwl/apiKey'
    inputBinding:
      position: 3
      prefix: -a
    type: string
  - id: '#annotationclient.cwl/assemblyName'
    inputBinding:
      position: 1
      prefix: -n
    type: string
  - id: '#annotationclient.cwl/binLineages'
    type:
    - 'null'
    - items: File
      type: array
  - format: http://edamontology.org/format_1929
    id: '#annotationclient.cwl/binnedFastas'
    type:
      items: File
      type: array
  - format: http://edamontology.org/format_3475
    id: '#annotationclient.cwl/checkmReport'
    type:
    - 'null'
    - File
  - format: http://edamontology.org/format_3475
    id: '#annotationclient.cwl/contigCoverage'
    type: File
  - format: http://edamontology.org/format_3475
    id: '#annotationclient.cwl/featureCountsPerSample'
    type:
      items: File
      type: array
  - format: http://edamontology.org/format_3475
    id: '#annotationclient.cwl/featureCountsTotal'
    type: File
  - id: '#annotationclient.cwl/hostURI'
    inputBinding:
      position: 2
      prefix: -h
    type: string
  - format: http://edamontology.org/format_2306
    id: '#annotationclient.cwl/predictedGenes'
    type: File
  - id: '#annotationclient.cwl/projectName'
    inputBinding:
      position: 4
      prefix: -p
    type: string
  - id: '#annotationclient.cwl/runIds'
    inputBinding:
      itemSeparator: ','
      position: 5
      prefix: -s
    type:
      items: string
      type: array
  label: MGX Annotate
  outputs:
  - id: '#annotationclient.cwl/success'
    outputBinding:
      outputEval: $(true)
    type: boolean
  requirements:
  - class: InlineJavascriptRequirement
  - class: InitialWorkDirRequirement
    listing:
    - $(inputs.checkmReport)
    - $(inputs.binnedFastas)
    - $(inputs.binLineages)
    - $(inputs.featureCountsPerSample)
    - $(inputs.predictedGenes)
    - $(inputs.featureCountsTotal)
    - $(inputs.contigCoverage)
  - class: ResourceRequirement
    coresMin: 2
    ramMin: 5000
- baseCommand: bamstats
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/bamstats
  id: '#bamstats.cwl'
  inputs:
  - format: http://edamontology.org/format_2572
    id: '#bamstats.cwl/bamFile'
    inputBinding:
      position: 1
    type: File
  - id: '#bamstats.cwl/outFile'
    inputBinding:
      position: 2
    type: string
  label: 'bamstats: BAM alignment statistics per reference sequence'
  outputs:
  - format: http://edamontology.org/format_3475
    id: '#bamstats.cwl/tsvOutput'
    outputBinding:
      glob: $(inputs.outFile)
    type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 1
    prefix: -o
    valueFrom: "${\n  return inputs.bamFile.nameroot.split(\"_\")[0]  + \".tsv\"\n\
      }\n"
  baseCommand: featureCounts
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/featurecounts
  id: '#featureCounts.cwl'
  inputs:
  - doc: Name of an annotation file. GTF format by default. See -F option for more
      formats.
    format: http://edamontology.org/format_2306
    id: '#featureCounts.cwl/annotation'
    inputBinding:
      position: 0
      prefix: -a
    type: File
  - default: gene_id
    doc: Specify attribute type in GTF annotation. `gene_id' by  default. Meta-features
      used for read counting will be extracted from annotation using the provided
      value.
    id: '#featureCounts.cwl/attribute_type'
    inputBinding:
      position: -10
      prefix: -g
    type: string
  - format: http://edamontology.org/format_2572
    id: '#featureCounts.cwl/bamFile'
    inputBinding:
      position: 1
    type: File
  - default: false
    doc: Count read pairs that have both ends successfully aligned  only.
    id: '#featureCounts.cwl/count_paired_map_only'
    inputBinding:
      position: -6
      prefix: -B
    type: boolean
  - default: false
    doc: Do not count read pairs that have their two ends mapping  to different chromosomes
      or mapping to same chromosome  but on different strands.
    id: '#featureCounts.cwl/discard_diff_chrom_mapping_pairs'
    inputBinding:
      position: -7
      prefix: -C
    type: boolean
  - default: false
    doc: Perform read counting at feature level (eg. counting  reads for exons rather
      than genes).
    id: '#featureCounts.cwl/feature_level'
    inputBinding:
      position: -11
      prefix: -f
    type: boolean
  - default: exon
    doc: Specify feature type in GTF annotation. `exon' by  default. Features used
      for read counting will be  extracted from annotation using the provided value.
    id: '#featureCounts.cwl/feature_type'
    inputBinding:
      position: -9
      prefix: -t
    type: string
  - default: GTF
    doc: Specify format of provided annotation file. Acceptable  formats include `GTF'
      and `SAF'. `GTF' by default. See  Users Guide for description of SAF format.
    id: '#featureCounts.cwl/file_format'
    inputBinding:
      position: -8
      prefix: -F
    type: string
  - default: false
    doc: Assign reads to a meta-feature/feature that has the  largest number of overlapping
      bases.
    id: '#featureCounts.cwl/largest_overlap'
    inputBinding:
      position: -16
      prefix: --largestOverlap
    type: boolean
  - default: 600
    doc: Maximum fragment/template length, 600 by default.
    id: '#featureCounts.cwl/max_fragment_length'
    inputBinding:
      position: -13
      prefix: -D
    type: int
  - default: 50
    doc: Minimum fragment/template length, 50 by default.
    id: '#featureCounts.cwl/min_fragment_length'
    inputBinding:
      position: -12
      prefix: -d
    type: int
  - default: 1
    doc: Specify minimum number of overlapping bases requried  between a read and
      a meta-feature/feature that the read  is assigned to. 1 by default.
    id: '#featureCounts.cwl/min_overlap'
    inputBinding:
      position: -15
      prefix: --minOverlap
    type: int
  - default: false
    doc: Multi-mapping reads will also be counted. For a multi- mapping read, all
      its reported alignments will be  counted. The `NH' tag in BAM/SAM input is used
      to detect  multi-mapping reads.
    id: '#featureCounts.cwl/multimapping'
    inputBinding:
      position: -17
      prefix: -M
    type: boolean
  - default: false
    doc: Assign reads to all their overlapping meta-features (or  features if -f is
      specified).
    id: '#featureCounts.cwl/overlap'
    inputBinding:
      position: -14
      prefix: -O
    type: boolean
  - default: false
    doc: Check validity of paired-end distance when counting read  pairs. Use -d and
      -D to set thresholds.
    id: '#featureCounts.cwl/pair_validity'
    inputBinding:
      position: -5
      prefix: -P
    type: boolean
  - default: true
    id: '#featureCounts.cwl/paired'
    inputBinding:
      position: -4
      prefix: -p
    type: boolean
  - default: false
    doc: Count fragments (read pairs) instead of individual reads.  For each read
      pair, its two reads must be adjacent to  each other in BAM/SAM input.
    id: '#featureCounts.cwl/pairs'
    inputBinding:
      position: -2
      prefix: -p
    type: boolean
  - default: false
    doc: Count primary alignments only. Primary alignments are  identified using bit
      0x100 in SAM/BAM FLAG field.
    id: '#featureCounts.cwl/primary_only'
    inputBinding:
      position: -3
      prefix: --primary
    type: boolean
  - default: 0
    doc: 'Perform strand-specific read counting. Possible values:   0 (unstranded),
      1 (stranded) and 2 (reversely stranded).  0 by default.'
    id: '#featureCounts.cwl/strand'
    inputBinding:
      position: -1
      prefix: -s
    type: int
  - default: 10
    doc: Number of the threads. 10 by default.
    id: '#featureCounts.cwl/threads'
    inputBinding:
      position: -18
      prefix: -T
    type: int
  outputs:
  - format: http://edamontology.org/format_3475
    id: '#featureCounts.cwl/output_counts'
    outputBinding:
      glob: $(inputs.bamFile.nameroot.split("_")[0]  + ".tsv")
    type: File
  requirements:
  - class: InlineJavascriptRequirement
- baseCommand: mergeFC
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  id: '#mergeFC.cwl'
  inputs:
  - format: http://edamontology.org/format_3475
    id: '#mergeFC.cwl/featureCountsTSV'
    inputBinding:
      itemSeparator: ','
      position: 1
      prefix: -i
    type:
      items: File
      type: array
  - id: '#mergeFC.cwl/outFile'
    inputBinding:
      position: 2
      prefix: -o
    type: string
  outputs:
  - format: http://edamontology.org/format_3475
    id: '#mergeFC.cwl/tsvOutput'
    outputBinding:
      glob: $(inputs.outFile)
    type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 1
    prefix: -f
    valueFrom: gff
  - position: 2
    prefix: -o
    valueFrom: "${\n  return inputs.inputFile.nameroot + \".gff\"\n}\n"
  - position: 3
    prefix: -a
    valueFrom: "${\n  return inputs.inputFile.nameroot + \".faa\"\n}\n"
  - position: 4
    prefix: -d
    valueFrom: "${\n  return inputs.inputFile.nameroot + \".fna\"\n}\n"
  baseCommand: pprodigal
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/prodigal
  id: '#prodigal.cwl'
  inputs:
  - default: 2000
    id: '#prodigal.cwl/chunkSize'
    inputBinding:
      position: 8
      prefix: -C
    type: int
  - id: '#prodigal.cwl/inputFile'
    inputBinding:
      position: 6
      prefix: -i
    type: File
  - default: false
    id: '#prodigal.cwl/metagenomic'
    inputBinding:
      position: 1
      prefix: -p
      valueFrom: meta
    type: boolean
  - default: 20
    id: '#prodigal.cwl/tasks'
    inputBinding:
      position: 7
      prefix: -T
    type: int
  label: Prodigal 2.6.3
  outputs:
  - format: http://edamontology.org/format_2306
    id: '#prodigal.cwl/annotations'
    outputBinding:
      glob: $(inputs.inputFile.nameroot + ".gff")
    type: File
  - format: http://edamontology.org/format_1929
    id: '#prodigal.cwl/genes'
    outputBinding:
      glob: $(inputs.inputFile.nameroot + ".fna")
    type: File
  - format: http://edamontology.org/format_1929
    id: '#prodigal.cwl/proteins'
    outputBinding:
      glob: $(inputs.inputFile.nameroot + ".faa")
    type: File
  requirements:
  - class: InlineJavascriptRequirement
  - class: ResourceRequirement
    coresMin: 20
    ramMin: 8000
- arguments:
  - position: 1
    prefix: -t
    valueFrom: '10'
  - position: 2
    prefix: -e
    valueFrom: rrna
  - position: 3
    prefix: -l
    valueFrom: '250'
  - position: 6
    prefix: -o
    valueFrom: ${ return inputs.fwdReads.nameroot + "_filtered.fq" }
  - position: 7
    valueFrom: ${ return inputs.revReads.nameroot + "_filtered.fq" }
  baseCommand: ribodetector
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/ribodetector
  id: '#ribodetector.cwl'
  inputs:
  - format: http://edamontology.org/format_1930
    id: '#ribodetector.cwl/fwdReads'
    inputBinding:
      position: 4
      prefix: -i
    type: File
  - format: http://edamontology.org/format_1930
    id: '#ribodetector.cwl/revReads'
    inputBinding:
      position: 5
      prefix: -i
    type:
    - 'null'
    - File
  label: RiboDetector
  outputs:
  - format: http://edamontology.org/format_1930
    id: '#ribodetector.cwl/fwdFiltered'
    outputBinding:
      glob: $(arguments.fwdReads.nameroot + "_filtered.fq")
    type: File
  - format: http://edamontology.org/format_1930
    id: '#ribodetector.cwl/revFiltered'
    outputBinding:
      glob: $(arguments.revReads.nameroot + "_filtered.fq")
    type:
    - 'null'
    - File
  requirements:
  - class: InlineJavascriptRequirement
  - class: ResourceRequirement
    coresMin: 10
    ramMin: 5000
- arguments:
  - position: 1
    prefix: -o
    valueFrom: rnaspades_out
  baseCommand: rnaspades.py
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  id: '#rnaspades.cwl'
  inputs:
  - id: '#rnaspades.cwl/mem-limit'
    inputBinding:
      position: 6
      prefix: -m
    type:
    - 'null'
    - int
  - format: http://edamontology.org/format_1930
    id: '#rnaspades.cwl/read1'
    inputBinding:
      position: 2
      prefix: '-1'
    type:
    - 'null'
    - items: File
      type: array
  - format: http://edamontology.org/format_1930
    id: '#rnaspades.cwl/read2'
    inputBinding:
      position: 3
      prefix: '-2'
    type:
    - 'null'
    - items: File
      type: array
  - id: '#rnaspades.cwl/thread-number'
    inputBinding:
      position: 5
      prefix: -t
    type:
    - 'null'
    - int
  - format: http://edamontology.org/format_1930
    id: '#rnaspades.cwl/unpaired'
    inputBinding:
      position: 4
      prefix: -s
    type:
    - 'null'
    - items: File
      type: array
  label: rnaSPAdes
  outputs:
  - format: http://edamontology.org/format_1929
    id: '#rnaspades.cwl/contigs'
    outputBinding:
      glob: rnaspades_out/transcripts.fasta
    type: File
- baseCommand:
  - samtools
  - merge
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/samtools
  id: '#samtools-merge.cwl'
  inputs:
  - format: http://edamontology.org/format_2572
    id: '#samtools-merge.cwl/inputs'
    inputBinding:
      position: 3
    type:
      items: File
      type: array
  - default: merged.bam
    id: '#samtools-merge.cwl/outFile'
    inputBinding:
      position: 2
    type: string
  - default: 10
    id: '#samtools-merge.cwl/thread-number'
    inputBinding:
      position: 1
      prefix: -@
    type:
    - 'null'
    - int
  outputs:
  - format: http://edamontology.org/format_2572
    id: '#samtools-merge.cwl/output'
    outputBinding:
      glob: $(inputs.outFile)
    type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 4
    prefix: -o
    valueFrom: "${\n  return inputs.input.nameroot + \".bam\"\n}\n"
  baseCommand:
  - samtools
  - view
  - -b
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/samtools
  id: '#samtools-sam2bam.cwl'
  inputs:
  - default: false
    id: '#samtools-sam2bam.cwl/fastcompression'
    inputBinding:
      position: 3
      prefix: '-1'
    type: boolean
  - id: '#samtools-sam2bam.cwl/input'
    inputBinding:
      position: 5
    type: File
  - default: 10
    id: '#samtools-sam2bam.cwl/thread-number'
    inputBinding:
      position: 2
      prefix: -@
    type:
    - 'null'
    - int
  - default: false
    id: '#samtools-sam2bam.cwl/uncompressed'
    inputBinding:
      position: 1
      prefix: -u
    type: boolean
  outputs:
  - id: '#samtools-sam2bam.cwl/output'
    outputBinding:
      glob: $(inputs.input.nameroot + ".bam")
    type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 2
    prefix: -o
    valueFrom: "${\n  return inputs.input.nameroot + \"_sorted.bam\"\n}\n"
  baseCommand:
  - samtools
  - sort
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/samtools
  id: '#samtools-sort.cwl'
  inputs:
  - id: '#samtools-sort.cwl/input'
    inputBinding:
      position: 3
    type: File
  - default: 10
    id: '#samtools-sort.cwl/thread-number'
    inputBinding:
      position: 1
      prefix: -@
    type:
    - 'null'
    - int
  outputs:
  - format: http://edamontology.org/format_2572
    id: '#samtools-sort.cwl/output'
    outputBinding:
      glob: $(inputs.input.nameroot + "_sorted.bam")
    type: File
  requirements:
  - class: InlineJavascriptRequirement
- baseCommand:
  - SeqRunFetcher
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  - class: DockerRequirement
    dockerPull: sjaenick/mgxannotate
  id: '#seqrunfetch.cwl'
  inputs:
  - id: '#seqrunfetch.cwl/apiKey'
    inputBinding:
      position: 2
      prefix: -a
    type: string
  - id: '#seqrunfetch.cwl/hostURI'
    inputBinding:
      position: 1
      prefix: -h
    type: string
  - id: '#seqrunfetch.cwl/projectName'
    inputBinding:
      position: 3
      prefix: -p
    type: string
  - id: '#seqrunfetch.cwl/runId'
    inputBinding:
      position: 5
      prefix: -r
    type: string
  label: MGX Fetch sequences
  outputs:
  - format: http://edamontology.org/format_1930
    id: '#seqrunfetch.cwl/fwdReads'
    outputBinding:
      glob: $(inputs.runId + "_R1.fq")
    type:
    - 'null'
    - File
  - format: http://edamontology.org/format_1930
    id: '#seqrunfetch.cwl/revReads'
    outputBinding:
      glob: $(inputs.runId + "_R2.fq")
    type:
    - 'null'
    - File
  - format: http://edamontology.org/format_1930
    id: '#seqrunfetch.cwl/singleReads'
    outputBinding:
      glob: $(inputs.runId + "_single.fq")
    type:
    - 'null'
    - File
  requirements:
  - class: ResourceRequirement
    coresMin: 1
    ramMin: 2500
  - class: InlineJavascriptRequirement
- arguments:
  - position: 3
    prefix: -o
    valueFrom: "${\n  return inputs.read1.nameroot.split(\"_\")[0] + \".sam\"\n}\n"
  baseCommand: strobealign
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  id: '#strobealign.cwl'
  inputs:
  - format: http://edamontology.org/format_1930
    id: '#strobealign.cwl/read1'
    inputBinding:
      position: 5
    type: File
  - format: http://edamontology.org/format_1930
    id: '#strobealign.cwl/read2'
    inputBinding:
      position: 6
    type:
    - 'null'
    - File
  - format: http://edamontology.org/format_1929
    id: '#strobealign.cwl/reference'
    inputBinding:
      position: 4
    type: File
  - default: true
    id: '#strobealign.cwl/skip_unaligned'
    inputBinding:
      position: 2
      prefix: -U
    type: boolean
  - default: 10
    id: '#strobealign.cwl/thread-number'
    inputBinding:
      position: 1
      prefix: -t
    type:
    - 'null'
    - int
  label: StrobeAlign - fast short read aligner
  outputs:
  - format: http://edamontology.org/format_2573
    id: '#strobealign.cwl/sam'
    outputBinding:
      glob: $(inputs.read1.nameroot.split("_")[0] + ".sam")
    type: File
  requirements:
  - class: InlineJavascriptRequirement
  - class: ResourceRequirement
    coresMin: 10
    ramMin: 3000
- arguments:
  - position: 5
    valueFrom: "${\n  inputs.fwdReads.nameroot + '_trimmed.fq'\n}\n"
  - position: 6
    valueFrom: "${\n  inputs.fwdReads.nameroot + '_unpaired_trimmed.fq'\n}\n"
  - position: 7
    valueFrom: "${\n  if (inputs.end_mode == \"PE\" && inputs.revReads)\n    return\
      \ inputs.revReads.nameroot + '_trimmed.fq'\n  return null;\n}\n"
  - position: 8
    valueFrom: "${\n  if (inputs.end_mode == \"PE\" && inputs.revReads)\n    return\
      \ inputs.revReads.nameroot + '_unpaired_trimmed.fq'\n  return null;\n}\n"
  - position: 9
    valueFrom: $("ILLUMINACLIP:" + inputs.input_adapters_file.path + ":"+ inputs.illuminaclip)
  baseCommand: trimmomatic
  class: CommandLineTool
  hints:
  - class: LoadListingRequirement
    loadListing: deep_listing
  - class: NetworkAccess
    networkAccess: true
  id: '#trimmomatic.cwl'
  inputs:
  - doc: 'SE|PE

      Single End (SE) or Paired End (PE) mode

      '
    id: '#trimmomatic.cwl/end_mode'
    inputBinding:
      position: 1
    type: string
  - default: 10
    id: '#trimmomatic.cwl/threads'
    inputBinding:
      position: 2
      prefix: -threads
    type: int
  - doc: FASTQ file for input read (read R1 in Paired End mode)
    format: http://edamontology.org/format_1930
    id: '#trimmomatic.cwl/fwdReads'
    inputBinding:
      position: 3
    type: File
  - doc: FASTQ file for read R2 in Paired End mode
    format: http://edamontology.org/format_1930
    id: '#trimmomatic.cwl/revReads'
    inputBinding:
      position: 4
    type:
    - 'null'
    - File
  - default: :2:30:10
    doc: '<fastaWithAdaptersEtc>:<seed mismatches>:<palindrome clip threshold>:<simple
      clip threshold>:<minAdapterLength>:<keepBothReads>

      Find and remove Illumina adapters.

      REQUIRED:

      <fastaWithAdaptersEtc>: specifies the path to a fasta file containing all the
      adapters, PCR sequences etc.

      The naming of the various sequences within this file determines how they are
      used. See below.

      <seedMismatches>: specifies the maximum mismatch count which will still allow
      a full match to be performed

      <palindromeClipThreshold>: specifies how accurate the match between the two
      ''adapter ligated'' reads must be

      for PE palindrome read alignment.

      <simpleClipThreshold>: specifies how accurate the match between any adapter
      etc. sequence must be against a read

      OPTIONAL:

      <minAdapterLength>: In addition to the alignment score, palindrome mode can
      verify

      that a minimum length of adapter has been detected. If unspecified, this defaults
      to 8 bases,

      for historical reasons. However, since palindrome mode has a very low false
      positive rate, this

      can be safely reduced, even down to 1, to allow shorter adapter fragments to
      be removed.

      <keepBothReads>: After read-though has been detected by palindrome mode, and
      the

      adapter sequence removed, the reverse read contains the same sequence information
      as the forward read, albeit in reverse complement. For this reason, the default
      behaviour is to entirely drop the reverse read. By specifying "true" for this
      parameter, the reverse read will also be retained, which may be useful e.g.
      if the downstream tools cannot handle a combination of paired and unpaired reads.

      '
    id: '#trimmomatic.cwl/illuminaclip'
    type: string
  - format: http://edamontology.org/format_1929
    id: '#trimmomatic.cwl/input_adapters_file'
    type: File
  - default: '4:20'
    doc: '<windowSize>:<requiredQuality>

      Perform a sliding window trimming, cutting once the average quality within the
      window falls

      below a threshold. By considering multiple bases, a single poor quality base
      will not cause the

      removal of high quality data later in the read.

      <windowSize>: specifies the number of bases to average across

      <requiredQuality>: specifies the average quality required

      '
    id: '#trimmomatic.cwl/slidingwindow'
    inputBinding:
      position: 10
      prefix: 'SLIDINGWINDOW:'
      separate: false
    type: string
  - default: 50
    id: '#trimmomatic.cwl/minlen'
    inputBinding:
      position: 11
      prefix: 'MINLEN:'
      separate: false
    type: int
  outputs:
  - format: http://edamontology.org/format_1930
    id: '#trimmomatic.cwl/fwdTrimmed'
    outputBinding:
      glob: $(inputs.fwdReads.nameroot + "_trimmed.fq")
    type: File
  - format: http://edamontology.org/format_1930
    id: '#trimmomatic.cwl/revTrimmed'
    outputBinding:
      glob: $(inputs.revReads.nameroot + "_trimmed.fq")
    type:
    - 'null'
    - File
  requirements:
  - class: ResourceRequirement
    coresMin: 10
    ramMin: 3000
  - class: InlineJavascriptRequirement
$namespaces:
  sbg: https://www.sevenbridges.com/
cwlVersion: v1.2
