class: Workflow
cwlVersion: v1.0
id: metagenome_assembly_pipeline
label: Metagenome Assembly and Quantification
$namespaces:
  sbg: 'https://www.sevenbridges.com/'
inputs:
  - id: assemblyName
    type: string
    doc: Name of the assembly
    'sbg:x': 2417.751220703125
    'sbg:y': 126.88172912597656
  - id: runIds
    type: 'string[]'
    doc: MGX seqrun IDs
    'sbg:x': -1214.8797607421875
    'sbg:y': -554.6783447265625
  - id: apiKey
    type: string
    'sbg:x': -1240.6788330078125
    'sbg:y': -165.33267211914062
  - id: projectName
    type: string
    'sbg:x': -1298.9215087890625
    'sbg:y': -400.0390319824219
  - id: hostURI
    type: string
    'sbg:x': -1176.080322265625
    'sbg:y': -298.3294982910156
  - id: taxonomyDirectory
    type: Directory
    'sbg:x': 1716.697509765625
    'sbg:y': -395.3261413574219
  - id: checkmDataDir
    type: Directory
    'sbg:x': 1310.708251953125
    'sbg:y': -735.12158203125
  - id: kraken2DatabaseDir
    type: Directory
    'sbg:x': 1340.703125
    'sbg:y': -93.4775619506836
  - id: dastoolDatabaseDir
    type: Directory
    'sbg:x': 822.3442993164062
    'sbg:y': -543.8947143554688
outputs:
  - id: success
    outputSource:
      - annotationclient/success
    type: boolean
    'sbg:x': 2941.834228515625
    'sbg:y': -51.07965850830078
steps:
  - id: fastp
    in:
      - id: read1
        source: seqrunfetch/fwdReads
      - id: read2
        source: seqrunfetch/revReads
    out:
      - id: reads1
      - id: reads2
    run: tools/fastp.cwl
    label: 'fastp: An ultra-fast all-in-one FASTQ preprocessor'
    scatter:
      - read1
      - read2
    scatterMethod: dotproduct
    'sbg:x': -700.0858154296875
    'sbg:y': -382.4306335449219
  - id: megahit
    in:
      - id: read1
        source:
          - fastp/reads1
      - id: read2
        source:
          - fastp/reads2
    out:
      - id: contigs
    run: tools/megahit.cwl
    label: 'MEGAHIT: metagenome assembly'
    'sbg:x': -132
    'sbg:y': -380.096923828125
  - id: bowtie2_build
    in:
      - id: reference
        source: megahit/contigs
    out:
      - id: index
    run: tools/bowtie2_build.cwl
    'sbg:x': -96.41685485839844
    'sbg:y': -153.67196655273438
  - id: samtools_sam2bam
    in:
      - id: input
        source: bowtie2/alignment
      - id: thread-number
        default: 8
    out:
      - id: output
    run: tools/samtools-sam2bam.cwl
    scatter:
      - input
    scatterMethod: dotproduct
    'sbg:x': 190.90786743164062
    'sbg:y': 23.49307632446289
  - id: samtools_sort
    in:
      - id: input
        source: samtools_sam2bam/output
      - id: thread-number
        default: 8
    out:
      - id: output
    run: tools/samtools-sort.cwl
    scatter:
      - input
    scatterMethod: dotproduct
    'sbg:x': 338.39495849609375
    'sbg:y': 23.79859161376953
  - id: metabat
    in:
      - id: bamfiles
        source:
          - samtools_sort/output
      - id: contigs
        source: megahit/contigs
      - id: save-class
        default: true
      - id: suppressBinOutput
        default: true
    out:
      - id: binAssignment
    run: tools/metabat.cwl
    label: 'MetaBAT: Metagenome Binning'
    'sbg:x': 608.19482421875
    'sbg:y': -153.64015197753906
  - id: bowtie2
    in:
      - id: index
        source:
          - bowtie2_build/index
      - id: output_file
        valueFrom: |
          ${   
            return inputs.read1.nameroot + "_mapped.sam"
          }
      - id: read1
        source:
          - fastp/reads1
        valueFrom: '$([self])'
      - id: read2
        source:
          - fastp/reads2
        valueFrom: '$([self])'
      - id: skip_unaligned
        default: true
      - id: thread-number
        default: 8
    out:
      - id: alignment
    run: tools/bowtie2.cwl
    scatter:
      - read1
      - read2
    scatterMethod: dotproduct
    'sbg:x': 29.385665893554688
    'sbg:y': 22.917165756225586
  - id: checkm
    in:
      - id: bin_suffix
        default: fas
      - id: binnedFastas
        source:
          - tsv2bins/binFastas
      - id: dataDir
        source: checkmDataDir
    out:
      - id: output
    run: tools/checkm.cwl
    'sbg:x': 1425.5784912109375
    'sbg:y': -645.318115234375
  - id: prodigal
    in:
      - id: inputFile
        source: tsv2bins/binFastas
    out:
      - id: annotations
      - id: genes
      - id: proteins
    run: tools/prodigal.cwl
    label: Prodigal 2.6.3
    scatter:
      - inputFile
    scatterMethod: dotproduct
    'sbg:x': 1275.5765380859375
    'sbg:y': -337.3270568847656
  - id: feature_counts
    in:
      - id: annotation
        source: prodigal_1/annotations
      - id: attribute_type
        default: ID
      - id: bamFile
        source: samtools_sort/output
      - id: feature_type
        default: CDS
    out:
      - id: output_counts
    run: tools/featureCounts.cwl
    scatter:
      - bamFile
    scatterMethod: dotproduct
    'sbg:x': 1588.5606689453125
    'sbg:y': 248.06858825683594
  - id: kraken2
    in:
      - id: databaseDir
        source: kraken2DatabaseDir
      - id: proteinQuery
        default: true
      - id: querySequences
        source: prodigal/proteins
      - id: thread-number
        default: 10
    out:
      - id: output
    run: tools/kraken2.cwl
    scatter:
      - querySequences
    scatterMethod: dotproduct
    'sbg:x': 1486.30517578125
    'sbg:y': -303.1912841796875
  - id: assign_bin
    in:
      - id: kraken2Output
        source: kraken2/output
      - id: taxonomyDirectory
        source: taxonomyDirectory
    out:
      - id: lineage
    run: tools/assignBin.cwl
    label: Taxonomic assignment of a metagenomic bin
    scatter:
      - kraken2Output
    scatterMethod: dotproduct
    'sbg:x': 1866.4539794921875
    'sbg:y': -317.1265563964844
  - id: concoct
    in:
      - id: bamFiles
        source:
          - samtools_sort/output
      - id: contigs
        source: megahit/contigs
      - id: threads
        default: 8
    out:
      - id: binAssignment
    run: tools/concoct.cwl
    label: CONCOCT
    'sbg:x': 602.8667602539062
    'sbg:y': -301.5096130371094
  - id: createtsvlist
    in:
      - id: file1
        source: metabat/binAssignment
      - id: file2
        source: concoct/binAssignment
    out:
      - id: files
    run: tools/CreateTSVList.cwl
    label: CreateTSVList
    'sbg:x': 783.26171875
    'sbg:y': -270.7899169921875
  - id: dastool
    in:
      - id: binAssignments
        source:
          - createtsvlist/files
      - id: contigs
        source: megahit/contigs
      - id: dastoolDatabaseDir
        source: dastoolDatabaseDir
      - id: threads
        default: 8
    out:
      - id: binTSV
    run: tools/dastool.cwl
    label: DAS tool
    'sbg:x': 956.87939453125
    'sbg:y': -388.86944580078125
  - id: samtools_merge
    in:
      - id: inputs
        source:
          - samtools_sort/output
      - id: outFile
        default: total_mapped.bam
    out:
      - id: output
    run: tools/samtools-merge.cwl
    'sbg:x': 1085.156982421875
    'sbg:y': 525.2728881835938
  - id: feature_counts_1
    in:
      - id: annotation
        source: prodigal_1/annotations
      - id: attribute_type
        default: ID
      - id: bamFile
        source: samtools_merge/output
      - id: feature_type
        default: CDS
      - id: outFile
        default: featureCounts_total.tsv
    out:
      - id: output_counts
    run: tools/featureCounts.cwl
    'sbg:x': 1837.6478271484375
    'sbg:y': 553.8271484375
  - id: bamstats
    in:
      - id: bamFile
        source: samtools_merge/output
    out:
      - id: tsvOutput
    run: tools/bamstats.cwl
    label: 'bamstats: BAM alignment statistics per reference sequence'
    'sbg:x': 1698.8221435546875
    'sbg:y': 788.7218017578125
  - id: tsv2bins
    in:
      - id: assembledContigs
        source: megahit/contigs
      - id: scaffold2bin
        source: dastool/binTSV
    out:
      - id: binFastas
    run: tools/tsv2bins.cwl
    label: TSV to binned FASTA
    'sbg:x': 1142.4676513671875
    'sbg:y': -451.083984375
  - id: prodigal_1
    in:
      - id: inputFile
        source: megahit/contigs
    out:
      - id: annotations
      - id: genes
      - id: proteins
    run: tools/prodigal.cwl
    label: Prodigal 2.6.3
    'sbg:x': 1294.3883056640625
    'sbg:y': 316.0826416015625
  - id: renamefile
    in:
      - id: srcfile
        source: feature_counts/output_counts
      - id: newname
        source: runIds
    out:
      - id: outfile
    run: tools/renamefile.cwl
    scatter:
      - srcfile
      - newname
    scatterMethod: dotproduct
    'sbg:x': 1835.0096435546875
    'sbg:y': 176.96951293945312
  - id: annotationclient
    in:
      - id: apiKey
        source: apiKey
      - id: assemblyName
        source: assemblyName
      - id: binLineages
        source:
          - assign_bin/lineage
      - id: binnedFastas
        source:
          - tsv2bins/binFastas
      - id: checkmReport
        source: checkm/output
      - id: contigCoverage
        source: bamstats/tsvOutput
      - id: featureCountsPerSample
        source:
          - renamefile/outfile
      - id: featureCountsTotal
        source: feature_counts_1/output_counts
      - id: hostURI
        source: hostURI
      - id: predictedGenes
        source: prodigal_1/annotations
      - id: projectName
        source: projectName
      - id: runIds
        source:
          - runIds
    out:
      - id: success
    run: tools/annotationclient.cwl
    label: MGX Annotate
    'sbg:x': 2666.7197265625
    'sbg:y': -52.03864669799805
  - id: seqrunfetch
    in:
      - id: apiKey
        source: apiKey
      - id: hostURI
        source: hostURI
      - id: projectName
        source: projectName
      - id: runId
        source: runIds
    out:
      - id: fwdReads
      - id: revReads
    run: tools/seqrunfetch.cwl
    label: MGX Fetch sequences
    scatter:
      - runId
    scatterMethod: dotproduct
    'sbg:x': -907.0060424804688
    'sbg:y': -383.3644714355469
requirements:
  - class: ScatterFeatureRequirement
  - class: MultipleInputFeatureRequirement
  - class: StepInputExpressionRequirement
  - class: InlineJavascriptRequirement

