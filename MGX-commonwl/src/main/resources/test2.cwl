$graph:
- $namespaces:
    sbg: https://www.sevenbridges.com/
  class: Workflow
  id: '#main'
  inputs:
  - doc: Name of the assembly
    https://www.sevenbridges.com/x: 2417.751220703125
    https://www.sevenbridges.com/y: 126.88172912597656
    id: '#main/assemblyName'
    type: string
  - doc: MGX seqrun IDs
    https://www.sevenbridges.com/x: -1214.8797607421875
    https://www.sevenbridges.com/y: -554.6783447265625
    id: '#main/runIds'
    type:
      items: string
      type: array
  - https://www.sevenbridges.com/x: -1240.6788330078125
    https://www.sevenbridges.com/y: -165.33267211914062
    id: '#main/apiKey'
    type: string
  - https://www.sevenbridges.com/x: -1298.9215087890625
    https://www.sevenbridges.com/y: -400.0390319824219
    id: '#main/projectName'
    type: string
  - https://www.sevenbridges.com/x: -1176.080322265625
    https://www.sevenbridges.com/y: -298.3294982910156
    id: '#main/hostURI'
    type: string
  - https://www.sevenbridges.com/x: 1716.697509765625
    https://www.sevenbridges.com/y: -395.3261413574219
    id: '#main/taxonomyDirectory'
    type: Directory
  - https://www.sevenbridges.com/x: 1310.708251953125
    https://www.sevenbridges.com/y: -735.12158203125
    id: '#main/checkmDataDir'
    type: Directory
  - https://www.sevenbridges.com/x: 1340.703125
    https://www.sevenbridges.com/y: -93.4775619506836
    id: '#main/kraken2DatabaseDir'
    type: Directory
  - https://www.sevenbridges.com/x: 822.3442993164062
    https://www.sevenbridges.com/y: -543.8947143554688
    id: '#main/dastoolDatabaseDir'
    type: Directory
  label: Metagenome Assembly and Quantification
  outputs:
  - https://www.sevenbridges.com/x: 2941.834228515625
    https://www.sevenbridges.com/y: -51.07965850830078
    id: '#main/success'
    outputSource:
    - '#main/annotationclient/success'
    type: boolean
  requirements:
  - class: ScatterFeatureRequirement
  - class: MultipleInputFeatureRequirement
  - class: StepInputExpressionRequirement
  - class: InlineJavascriptRequirement
  steps:
  - https://www.sevenbridges.com/x: -700.0858154296875
    https://www.sevenbridges.com/y: -382.4306335449219
    id: '#main/fastp'
    in:
    - id: '#main/fastp/read1'
      source: '#main/seqrunfetch/fwdReads'
    - id: '#main/fastp/read2'
      source: '#main/seqrunfetch/revReads'
    label: 'fastp: An ultra-fast all-in-one FASTQ preprocessor'
    out:
    - id: '#main/fastp/reads1'
    - id: '#main/fastp/reads2'
    run: '#fastp.cwl'
    scatter:
    - '#main/fastp/read1'
    - '#main/fastp/read2'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: -132
    https://www.sevenbridges.com/y: -380.096923828125
    id: '#main/megahit'
    in:
    - id: '#main/megahit/read1'
      source:
      - '#main/fastp/reads1'
    - id: '#main/megahit/read2'
      source:
      - '#main/fastp/reads2'
    label: 'MEGAHIT: metagenome assembly'
    out:
    - id: '#main/megahit/contigs'
    run: '#megahit.cwl'
  - https://www.sevenbridges.com/x: -96.41685485839844
    https://www.sevenbridges.com/y: -153.67196655273438
    id: '#main/bowtie2_build'
    in:
    - id: '#main/bowtie2_build/reference'
      source: '#main/megahit/contigs'
    out:
    - id: '#main/bowtie2_build/index'
    run: '#bowtie2_build.cwl'
  - https://www.sevenbridges.com/x: 190.90786743164062
    https://www.sevenbridges.com/y: 23.49307632446289
    id: '#main/samtools_sam2bam'
    in:
    - id: '#main/samtools_sam2bam/input'
      source: '#main/bowtie2/alignment'
    - default: 8
      id: '#main/samtools_sam2bam/thread-number'
    out:
    - id: '#main/samtools_sam2bam/output'
    run: '#samtools-sam2bam.cwl'
    scatter:
    - '#main/samtools_sam2bam/input'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 338.39495849609375
    https://www.sevenbridges.com/y: 23.79859161376953
    id: '#main/samtools_sort'
    in:
    - id: '#main/samtools_sort/input'
      source: '#main/samtools_sam2bam/output'
    - default: 8
      id: '#main/samtools_sort/thread-number'
    out:
    - id: '#main/samtools_sort/output'
    run: '#samtools-sort.cwl'
    scatter:
    - '#main/samtools_sort/input'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 608.19482421875
    https://www.sevenbridges.com/y: -153.64015197753906
    id: '#main/metabat'
    in:
    - id: '#main/metabat/bamfiles'
      source:
      - '#main/samtools_sort/output'
    - id: '#main/metabat/contigs'
      source: '#main/megahit/contigs'
    - default: true
      id: '#main/metabat/save-class'
    - default: true
      id: '#main/metabat/suppressBinOutput'
    label: 'MetaBAT: Metagenome Binning'
    out:
    - id: '#main/metabat/binAssignment'
    run: '#metabat.cwl'
  - https://www.sevenbridges.com/x: 29.385665893554688
    https://www.sevenbridges.com/y: 22.917165756225586
    id: '#main/bowtie2'
    in:
    - id: '#main/bowtie2/index'
      source:
      - '#main/bowtie2_build/index'
    - id: '#main/bowtie2/output_file'
      valueFrom: "${   \n  return inputs.read1.nameroot + \"_mapped.sam\"\n}\n"
    - id: '#main/bowtie2/read1'
      source:
      - '#main/fastp/reads1'
      valueFrom: $([self])
    - id: '#main/bowtie2/read2'
      source:
      - '#main/fastp/reads2'
      valueFrom: $([self])
    - default: true
      id: '#main/bowtie2/skip_unaligned'
    - default: 8
      id: '#main/bowtie2/thread-number'
    out:
    - id: '#main/bowtie2/alignment'
    run: '#bowtie2.cwl'
    scatter:
    - '#main/bowtie2/read1'
    - '#main/bowtie2/read2'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1425.5784912109375
    https://www.sevenbridges.com/y: -645.318115234375
    id: '#main/checkm'
    in:
    - default: fas
      id: '#main/checkm/bin_suffix'
    - id: '#main/checkm/binnedFastas'
      source:
      - '#main/tsv2bins/binFastas'
    - id: '#main/checkm/dataDir'
      source: '#main/checkmDataDir'
    out:
    - id: '#main/checkm/output'
    run: '#checkm.cwl'
  - https://www.sevenbridges.com/x: 1275.5765380859375
    https://www.sevenbridges.com/y: -337.3270568847656
    id: '#main/prodigal'
    in:
    - id: '#main/prodigal/inputFile'
      source: '#main/tsv2bins/binFastas'
    label: Prodigal 2.6.3
    out:
    - id: '#main/prodigal/annotations'
    - id: '#main/prodigal/genes'
    - id: '#main/prodigal/proteins'
    run: '#prodigal.cwl'
    scatter:
    - '#main/prodigal/inputFile'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1588.5606689453125
    https://www.sevenbridges.com/y: 248.06858825683594
    id: '#main/feature_counts'
    in:
    - id: '#main/feature_counts/annotation'
      source: '#main/prodigal_1/annotations'
    - default: ID
      id: '#main/feature_counts/attribute_type'
    - id: '#main/feature_counts/bamFile'
      source: '#main/samtools_sort/output'
    - default: CDS
      id: '#main/feature_counts/feature_type'
    out:
    - id: '#main/feature_counts/output_counts'
    run: '#featureCounts.cwl'
    scatter:
    - '#main/feature_counts/bamFile'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1486.30517578125
    https://www.sevenbridges.com/y: -303.1912841796875
    id: '#main/kraken2'
    in:
    - id: '#main/kraken2/databaseDir'
      source: '#main/kraken2DatabaseDir'
    - default: true
      id: '#main/kraken2/proteinQuery'
    - id: '#main/kraken2/querySequences'
      source: '#main/prodigal/proteins'
    - default: 10
      id: '#main/kraken2/thread-number'
    out:
    - id: '#main/kraken2/output'
    run: '#kraken2.cwl'
    scatter:
    - '#main/kraken2/querySequences'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 1866.4539794921875
    https://www.sevenbridges.com/y: -317.1265563964844
    id: '#main/assign_bin'
    in:
    - id: '#main/assign_bin/kraken2Output'
      source: '#main/kraken2/output'
    - id: '#main/assign_bin/taxonomyDirectory'
      source: '#main/taxonomyDirectory'
    label: Taxonomic assignment of a metagenomic bin
    out:
    - id: '#main/assign_bin/lineage'
    run: '#assignBin.cwl'
    scatter:
    - '#main/assign_bin/kraken2Output'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 602.8667602539062
    https://www.sevenbridges.com/y: -301.5096130371094
    id: '#main/concoct'
    in:
    - id: '#main/concoct/bamFiles'
      source:
      - '#main/samtools_sort/output'
    - id: '#main/concoct/contigs'
      source: '#main/megahit/contigs'
    - default: 8
      id: '#main/concoct/threads'
    label: CONCOCT
    out:
    - id: '#main/concoct/binAssignment'
    run: '#concoct.cwl'
  - https://www.sevenbridges.com/x: 783.26171875
    https://www.sevenbridges.com/y: -270.7899169921875
    id: '#main/createtsvlist'
    in:
    - id: '#main/createtsvlist/file1'
      source: '#main/metabat/binAssignment'
    - id: '#main/createtsvlist/file2'
      source: '#main/concoct/binAssignment'
    label: CreateTSVList
    out:
    - id: '#main/createtsvlist/files'
    run: '#CreateTSVList.cwl'
  - https://www.sevenbridges.com/x: 956.87939453125
    https://www.sevenbridges.com/y: -388.86944580078125
    id: '#main/dastool'
    in:
    - id: '#main/dastool/binAssignments'
      source:
      - '#main/createtsvlist/files'
    - id: '#main/dastool/contigs'
      source: '#main/megahit/contigs'
    - id: '#main/dastool/dastoolDatabaseDir'
      source: '#main/dastoolDatabaseDir'
    - default: 8
      id: '#main/dastool/threads'
    label: DAS tool
    out:
    - id: '#main/dastool/binTSV'
    run: '#dastool.cwl'
  - https://www.sevenbridges.com/x: 1085.156982421875
    https://www.sevenbridges.com/y: 525.2728881835938
    id: '#main/samtools_merge'
    in:
    - id: '#main/samtools_merge/inputs'
      source:
      - '#main/samtools_sort/output'
    - default: total_mapped.bam
      id: '#main/samtools_merge/outFile'
    out:
    - id: '#main/samtools_merge/output'
    run: '#samtools-merge.cwl'
  - https://www.sevenbridges.com/x: 1837.6478271484375
    https://www.sevenbridges.com/y: 553.8271484375
    id: '#main/feature_counts_1'
    in:
    - id: '#main/feature_counts_1/annotation'
      source: '#main/prodigal_1/annotations'
    - default: ID
      id: '#main/feature_counts_1/attribute_type'
    - id: '#main/feature_counts_1/bamFile'
      source: '#main/samtools_merge/output'
    - default: CDS
      id: '#main/feature_counts_1/feature_type'
    - default: featureCounts_total.tsv
      id: '#main/feature_counts_1/outFile'
    out:
    - id: '#main/feature_counts_1/output_counts'
    run: '#featureCounts.cwl'
  - https://www.sevenbridges.com/x: 1698.8221435546875
    https://www.sevenbridges.com/y: 788.7218017578125
    id: '#main/bamstats'
    in:
    - id: '#main/bamstats/bamFile'
      source: '#main/samtools_merge/output'
    label: 'bamstats: BAM alignment statistics per reference sequence'
    out:
    - id: '#main/bamstats/tsvOutput'
    run: '#bamstats.cwl'
  - https://www.sevenbridges.com/x: 1142.4676513671875
    https://www.sevenbridges.com/y: -451.083984375
    id: '#main/tsv2bins'
    in:
    - id: '#main/tsv2bins/assembledContigs'
      source: '#main/megahit/contigs'
    - id: '#main/tsv2bins/scaffold2bin'
      source: '#main/dastool/binTSV'
    label: TSV to binned FASTA
    out:
    - id: '#main/tsv2bins/binFastas'
    run: '#tsv2bins.cwl'
  - https://www.sevenbridges.com/x: 1294.3883056640625
    https://www.sevenbridges.com/y: 316.0826416015625
    id: '#main/prodigal_1'
    in:
    - id: '#main/prodigal_1/inputFile'
      source: '#main/megahit/contigs'
    label: Prodigal 2.6.3
    out:
    - id: '#main/prodigal_1/annotations'
    - id: '#main/prodigal_1/genes'
    - id: '#main/prodigal_1/proteins'
    run: '#prodigal.cwl'
  - https://www.sevenbridges.com/x: 1835.0096435546875
    https://www.sevenbridges.com/y: 176.96951293945312
    id: '#main/renamefile'
    in:
    - id: '#main/renamefile/srcfile'
      source: '#main/feature_counts/output_counts'
    - id: '#main/renamefile/newname'
      source: '#main/runIds'
    out:
    - id: '#main/renamefile/outfile'
    run: '#renamefile.cwl'
    scatter:
    - '#main/renamefile/srcfile'
    - '#main/renamefile/newname'
    scatterMethod: dotproduct
  - https://www.sevenbridges.com/x: 2666.7197265625
    https://www.sevenbridges.com/y: -52.03864669799805
    id: '#main/annotationclient'
    in:
    - id: '#main/annotationclient/apiKey'
      source: '#main/apiKey'
    - id: '#main/annotationclient/assemblyName'
      source: '#main/assemblyName'
    - id: '#main/annotationclient/binLineages'
      source:
      - '#main/assign_bin/lineage'
    - id: '#main/annotationclient/binnedFastas'
      source:
      - '#main/tsv2bins/binFastas'
    - id: '#main/annotationclient/checkmReport'
      source: '#main/checkm/output'
    - id: '#main/annotationclient/contigCoverage'
      source: '#main/bamstats/tsvOutput'
    - id: '#main/annotationclient/featureCountsPerSample'
      source:
      - '#main/renamefile/outfile'
    - id: '#main/annotationclient/featureCountsTotal'
      source: '#main/feature_counts_1/output_counts'
    - id: '#main/annotationclient/hostURI'
      source: '#main/hostURI'
    - id: '#main/annotationclient/predictedGenes'
      source: '#main/prodigal_1/annotations'
    - id: '#main/annotationclient/projectName'
      source: '#main/projectName'
    - id: '#main/annotationclient/runIds'
      source:
      - '#main/runIds'
    label: MGX Annotate
    out:
    - id: '#main/annotationclient/success'
    run: '#annotationclient.cwl'
  - https://www.sevenbridges.com/x: -907.0060424804688
    https://www.sevenbridges.com/y: -383.3644714355469
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
    run: '#seqrunfetch.cwl'
    scatter:
    - '#main/seqrunfetch/runId'
    scatterMethod: dotproduct
- class: ExpressionTool
  expression: "${\n  var files = [];\n  if (inputs.file1[\"size\"] && inputs.file1[\"\
    size\"] != 0) {\n      files.push(inputs.file1)\n  }\n  if (inputs.file2[\"size\"\
    ] && inputs.file2[\"size\"] != 0) {\n      files.push(inputs.file2)\n  }\n  return\
    \ {\"files\": files};\n}\n"
  id: '#CreateTSVList.cwl'
  inputs:
    file1:
      format: http://edamontology.org/format_3475
      type: File
    file2:
      format: http://edamontology.org/format_3475
      type: File
  label: CreateTSVList
  outputs:
    files:
      type:
        items: File
        type: array
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 5
    prefix: -d
    valueFrom: $(runtime.outdir)
  baseCommand:
  - AnnotationClient
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/mgxannotate
  id: '#annotationclient.cwl'
  inputs:
    apiKey:
      inputBinding:
        position: 3
        prefix: -a
      type: string
    assemblyName:
      inputBinding:
        position: 1
        prefix: -n
      type: string
    binLineages:
      type: File[]
    binnedFastas:
      format: http://edamontology.org/format_1929
      type: File[]
    checkmReport:
      format: http://edamontology.org/format_3475
      type: File
    contigCoverage:
      format: http://edamontology.org/format_3475
      type: File
    featureCountsPerSample:
      format: http://edamontology.org/format_3475
      type: File[]
    featureCountsTotal:
      format: http://edamontology.org/format_3475
      type: File
    hostURI:
      inputBinding:
        position: 2
        prefix: -h
      type: string
    predictedGenes:
      format: http://edamontology.org/format_2306
      type: File
    projectName:
      inputBinding:
        position: 4
        prefix: -p
      type: string
    runIds:
      inputBinding:
        itemSeparator: ','
        position: 5
        prefix: -s
      type: string[]
  label: MGX Annotate
  outputs:
    success:
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
- arguments:
  - position: 5
    valueFrom: "${\n  return inputs.kraken2Output.nameroot + \".tax\"\n}\n"
  baseCommand: assignBin.pl
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/smallscripts
  id: '#assignBin.cwl'
  inputs:
    fractionCutoff:
      default: '0.8'
      inputBinding:
        position: 3
      type: string
    kraken2Output:
      format: http://edamontology.org/format_3475
      inputBinding:
        position: 1
      type: File
    minNumber:
      default: 5
      inputBinding:
        position: 4
      type: int
    taxonomyDirectory:
      inputBinding:
        position: 2
      type: Directory
  label: Taxonomic assignment of a metagenomic bin
  outputs:
    lineage:
      outputBinding:
        glob: $(inputs.kraken2Output.nameroot + ".tax")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 2
    valueFrom: "${\n  return inputs.bamFile.nameroot + \".cov\"\n}\n"
  baseCommand: bamstats
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/bamstats
  id: '#bamstats.cwl'
  inputs:
    bamFile:
      format: http://edamontology.org/format_2572
      inputBinding:
        position: 1
      type: File
  label: 'bamstats: BAM alignment statistics per reference sequence'
  outputs:
    tsvOutput:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $(inputs.bamFile.nameroot + ".cov")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- baseCommand:
  - bowtie2
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/bowtie2
  id: '#bowtie2.cwl'
  inputs:
    index:
      format: http://edamontology.org/format_3326
      inputBinding:
        position: 2
        prefix: -x
      type:
        inputBinding:
          valueFrom: "${\n  if (self.basename == inputs.index[0].basename) {\n   \
            \ var split = self.basename.split('.');\n    return split.slice(0, split.length\
            \ - 2).join('.');\n  } else {\n    return null;\n  }\n}\n"
        items: File
        type: array
    output_file:
      default: mapped.sam
      inputBinding:
        position: 6
        prefix: -S
      type: string
    read1:
      format: http://edamontology.org/format_1930
      inputBinding:
        itemSeparator: ','
        position: 3
        prefix: '-1'
      type: File[]?
    read2:
      format: http://edamontology.org/format_1930
      inputBinding:
        itemSeparator: ','
        position: 4
        prefix: '-2'
      type: File[]?
    skip_unaligned:
      default: true
      inputBinding:
        position: 5
        prefix: --no-unal
      type: boolean
    thread-number:
      default: 10
      inputBinding:
        position: 1
        prefix: --threads
      type: int?
  outputs:
    alignment:
      format: http://edamontology.org/format_2572
      outputBinding:
        glob: $(inputs.output_file)
      type: File
  requirements:
  - class: InlineJavascriptRequirement
  - class: InitialWorkDirRequirement
    listing:
    - $(inputs.index[0])
    - $(inputs.index[1])
    - $(inputs.index[2])
    - $(inputs.index[3])
    - $(inputs.index[4])
    - $(inputs.index[5])
- baseCommand:
  - bowtie2-build
  class: CommandLineTool
  doc: 'Usage:   bowtie2-build [options]* <reference_in> <bt2_base>

    '
  hints:
    DockerRequirement:
      dockerPull: sjaenick/bowtie2
  id: '#bowtie2_build.cwl'
  inputs:
    output_prefix:
      default: default
      doc: 'Specify a filename prefix for the reference genome index. Default: use
        the filename prefix of the reference

        '
      inputBinding:
        position: 3
        valueFrom: "${\n  if (self == \"default\") {\n    return inputs.reference.nameroot;\n\
          \  } else {\n    return self;\n  }\n}\n"
      type: string?
    reference:
      format: http://edamontology.org/format_1929
      inputBinding:
        position: 1
      type: File
    thread-number:
      default: 6
      inputBinding:
        position: 2
        prefix: --threads
      type: int?
  outputs:
    index:
      format: http://edamontology.org/format_3326
      outputBinding:
        glob:
        - "${\n  if (inputs.output_prefix == \"default\") {\n    return inputs.reference.nameroot\
          \ + \".1.bt2\"\n  } else {\n    return inputs.output_prefix + \".1.bt2\"\
          \n  }\n}\n"
        - "${\n  if (inputs.output_prefix == \"default\") {\n    return inputs.reference.nameroot\
          \ + \".2.bt2\"\n  } else {\n    return inputs.output_prefix + \".2.bt2\"\
          \n  }\n}\n"
        - "${\n  if (inputs.output_prefix == \"default\") {\n    return inputs.reference.nameroot\
          \ + \".3.bt2\"\n  } else {\n    return inputs.output_prefix + \".3.bt2\"\
          \n  }\n}\n"
        - "${\n  if (inputs.output_prefix == \"default\") {\n    return inputs.reference.nameroot\
          \ + \".4.bt2\"\n  } else {\n    return inputs.output_prefix + \".4.bt2\"\
          \n  }\n}\n"
        - "${\n  if (inputs.output_prefix == \"default\") {\n    return inputs.reference.nameroot\
          \ + \".rev.1.bt2\"\n  } else {\n    return inputs.output_prefix + \".rev.1.bt2\"\
          \n  }\n}\n"
        - "${\n  if (inputs.output_prefix == \"default\") {\n    return inputs.reference.nameroot\
          \ + \".rev.2.bt2\"\n  } else {\n    return inputs.output_prefix + \".rev.2.bt2\"\
          \n  }\n}\n"
      type:
        items: File
        type: array
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 5
    valueFrom: .
  baseCommand:
  - checkm
  - lineage_wf
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/checkm
  id: '#checkm.cwl'
  inputs:
    bin_suffix:
      default: fa
      inputBinding:
        position: 2
        prefix: -x
      type: string
    binnedFastas:
      type: File[]
    dataDir:
      type: Directory
    outdir:
      default: checkm_out
      inputBinding:
        position: 6
      type: string
    report_file:
      default: checkm.tsv
      inputBinding:
        position: 3
        prefix: -f
      type: string
    tabular_output:
      default: true
      inputBinding:
        position: 4
        prefix: --tab_table
      type: boolean
    thread-number:
      default: 6
      inputBinding:
        position: 1
        prefix: -t
      type: int?
  outputs:
    output:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $(inputs.report_file)
      type: File
  requirements:
  - class: InitialWorkDirRequirement
    listing:
    - $(inputs.dataDir)
    - $(inputs.binnedFastas)
- baseCommand: runCONCOCT
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/concoct
  id: '#concoct.cwl'
  inputs:
    bamFiles:
      format: http://edamontology.org/format_2572
      inputBinding:
        position: 3
      type: File[]
    contigs:
      format: http://edamontology.org/format_1929
      inputBinding:
        position: 1
      type: File
    threads:
      default: 6
      inputBinding:
        position: 2
      type: int
  label: CONCOCT
  outputs:
    binAssignment:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $("concoct.scaffolds2bin.tsv")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
  - class: InitialWorkDirRequirement
    listing: $(inputs.bamFiles)
- baseCommand: DAS_Tool
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/dastool
  id: '#dastool.cwl'
  inputs:
    binAssignments:
      format: http://edamontology.org/format_3475
      inputBinding:
        itemSeparator: ','
        position: 1
        prefix: -i
      type: File[]?
    contigs:
      format: http://edamontology.org/format_1929
      inputBinding:
        position: 2
        prefix: -c
      type: File
    createPlots:
      default: false
      inputBinding:
        position: 4
        prefix: --create_plots
      type: boolean
    dastoolDatabaseDir:
      inputBinding:
        position: 5
        prefix: --db_directory
      type: Directory
    outPrefix:
      default: DASTool
      inputBinding:
        position: 3
        prefix: -o
      type: string
    searchEngine:
      default: diamond
      inputBinding:
        position: 6
        prefix: --search_engine
      type: string
    threads:
      inputBinding:
        position: 7
        prefix: -t
      type: int?
    writeBinEvals:
      default: 1
      inputBinding:
        position: 8
        prefix: --write_bin_evals
      type: int
  label: DAS tool
  outputs:
    binTSV:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $(inputs.outPrefix + "_DASTool_scaffolds2bin.txt")
      type: File
- arguments:
  - position: 3
    prefix: --out1
    valueFrom: "${\n  return inputs.read1.nameroot + \"_trimmed.fq\"\n}\n"
  - position: 4
    prefix: --out2
    valueFrom: "${\n  return inputs.read2.nameroot + \"_trimmed.fq\"\n}\n"
  baseCommand: fastp
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/fastp
  id: '#fastp.cwl'
  inputs:
    detect-paired-end-adapter:
      inputBinding:
        position: 10
        prefix: --detect_adapter_for_pe
      type: boolean?
    disable-adapter-trimming:
      inputBinding:
        position: 9
        prefix: --disable_adapter_trimming
      type: boolean?
    disable-quality-filter:
      inputBinding:
        position: 7
        prefix: --disable_quality_filtering
      type: boolean?
    enable-low-complexity-filter:
      inputBinding:
        position: 8
        prefix: --low_complexity_filter
      type: boolean?
    html_report:
      default: /dev/null
      inputBinding:
        position: 15
        prefix: --html
      type: string
    json_report:
      default: /dev/null
      inputBinding:
        position: 14
        prefix: --json
      type: string
    max-length:
      inputBinding:
        position: 13
        prefix: --length_limit
      type: int?
    min-length:
      inputBinding:
        position: 12
        prefix: --length_required
      type: int?
    read1:
      format: http://edamontology.org/format_1930
      inputBinding:
        position: 1
        prefix: --in1
      type: File
    read2:
      format: http://edamontology.org/format_1930
      inputBinding:
        position: 2
        prefix: --in2
      type: File?
    thread-number:
      default: 8
      inputBinding:
        position: 11
        prefix: --thread
      type: int?
    trim-poly-g:
      inputBinding:
        position: 5
        prefix: --trim_poly_g
      type: boolean?
    trim-poly-x:
      inputBinding:
        position: 6
        prefix: --trim_poly_x
      type: boolean?
  label: 'fastp: An ultra-fast all-in-one FASTQ preprocessor'
  outputs:
    reads1:
      format: http://edamontology.org/format_1930
      outputBinding:
        glob: $(inputs.read1.nameroot + "_trimmed.fq")
      type: File
    reads2:
      format: http://edamontology.org/format_1930
      outputBinding:
        glob: $(inputs.read2.nameroot + "_trimmed.fq")
      type: File?
  requirements:
  - class: InlineJavascriptRequirement
- baseCommand: featureCounts
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/featurecounts
  id: '#featureCounts.cwl'
  inputs:
    annotation:
      doc: Name of an annotation file. GTF format by default. See -F option for more
        formats.
      format: http://edamontology.org/format_2306
      inputBinding:
        position: 0
        prefix: -a
      type: File
    attribute_type:
      default: gene_id
      doc: Specify attribute type in GTF annotation. `gene_id' by  default. Meta-features
        used for read counting will be extracted from annotation using the provided
        value.
      inputBinding:
        position: -10
        prefix: -g
      type: string
    bamFile:
      format: http://edamontology.org/format_2572
      inputBinding:
        position: 1
      type: File
    count_paired_map_only:
      default: false
      doc: Count read pairs that have both ends successfully aligned  only.
      inputBinding:
        position: -6
        prefix: -B
      type: boolean
    discard_diff_chrom_mapping_pairs:
      default: false
      doc: Do not count read pairs that have their two ends mapping  to different
        chromosomes or mapping to same chromosome  but on different strands.
      inputBinding:
        position: -7
        prefix: -C
      type: boolean
    feature_level:
      default: false
      doc: Perform read counting at feature level (eg. counting  reads for exons rather
        than genes).
      inputBinding:
        position: -11
        prefix: -f
      type: boolean
    feature_type:
      default: exon
      doc: Specify feature type in GTF annotation. `exon' by  default. Features used
        for read counting will be  extracted from annotation using the provided value.
      inputBinding:
        position: -9
        prefix: -t
      type: string
    file_format:
      default: GTF
      doc: Specify format of provided annotation file. Acceptable  formats include
        `GTF' and `SAF'. `GTF' by default. See  Users Guide for description of SAF
        format.
      inputBinding:
        position: -8
        prefix: -F
      type: string
    largest_overlap:
      default: false
      doc: Assign reads to a meta-feature/feature that has the  largest number of
        overlapping bases.
      inputBinding:
        position: -16
        prefix: --largestOverlap
      type: boolean
    max_fragment_length:
      default: 600
      doc: Maximum fragment/template length, 600 by default.
      inputBinding:
        position: -13
        prefix: -D
      type: int
    min_fragment_length:
      default: 50
      doc: Minimum fragment/template length, 50 by default.
      inputBinding:
        position: -12
        prefix: -d
      type: int
    min_overlap:
      default: 1
      doc: Specify minimum number of overlapping bases requried  between a read and
        a meta-feature/feature that the read  is assigned to. 1 by default.
      inputBinding:
        position: -15
        prefix: --minOverlap
      type: int
    multimapping:
      default: false
      doc: Multi-mapping reads will also be counted. For a multi- mapping read, all
        its reported alignments will be  counted. The `NH' tag in BAM/SAM input is
        used to detect  multi-mapping reads.
      inputBinding:
        position: -17
        prefix: -M
      type: boolean
    orientation:
      default: fr
      doc: Specify orientation of two reads from the same pair, 'fr'  by by default
        (forward/reverse).
      inputBinding:
        position: -4
        prefix: -S
      type: string
    outFile:
      default: featurecounts.tsv
      inputBinding:
        position: 1
        prefix: -o
      type: string
    overlap:
      default: false
      doc: Assign reads to all their overlapping meta-features (or  features if -f
        is specified).
      inputBinding:
        position: -14
        prefix: -O
      type: boolean
    pair_validity:
      default: false
      doc: Check validity of paired-end distance when counting read  pairs. Use -d
        and -D to set thresholds.
      inputBinding:
        position: -5
        prefix: -P
      type: boolean
    pairs:
      default: false
      doc: Count fragments (read pairs) instead of individual reads.  For each read
        pair, its two reads must be adjacent to  each other in BAM/SAM input.
      inputBinding:
        position: -2
        prefix: -p
      type: boolean
    primary_only:
      default: false
      doc: Count primary alignments only. Primary alignments are  identified using
        bit 0x100 in SAM/BAM FLAG field.
      inputBinding:
        position: -3
        prefix: --primary
      type: boolean
    strand:
      default: 0
      doc: 'Perform strand-specific read counting. Possible values:   0 (unstranded),
        1 (stranded) and 2 (reversely stranded).  0 by default.'
      inputBinding:
        position: -1
        prefix: -s
      type: int
    threads:
      default: 8
      doc: Number of the threads. 8 by default.
      inputBinding:
        position: -18
        prefix: -T
      type: int
  outputs:
    output_counts:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $(inputs.outFile)
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 4
    prefix: --output
    valueFrom: "${\n  return inputs.querySequences.nameroot + \".krkn\"\n}\n"
  baseCommand: kraken2
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/kraken2
  id: '#kraken2.cwl'
  inputs:
    databaseDir:
      inputBinding:
        position: 1
        prefix: --db
      type: Directory
    proteinQuery:
      default: false
      inputBinding:
        position: 3
        prefix: --protein-query
      type: boolean
    querySequences:
      format: http://edamontology.org/format_1929
      inputBinding:
        position: 5
      type: File
    thread-number:
      default: 10
      inputBinding:
        position: 2
        prefix: --threads
      type: int?
  outputs:
    output:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $(inputs.querySequences.nameroot + ".krkn")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
  - class: InitialWorkDirRequirement
    listing:
    - $(inputs.databaseDir)
- baseCommand: megahit
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/megahit
  id: '#megahit.cwl'
  inputs:
    interleaved:
      format: http://edamontology.org/format_1930
      inputBinding:
        itemSeparator: ','
        position: 3
        prefix: --12
      type: File[]?
    min-contig-length:
      default: 1000
      inputBinding:
        position: 7
        prefix: --min-contig-len
      type: int?
    presets:
      inputBinding:
        position: 8
        prefix: --presets
      type: string?
    read1:
      format: http://edamontology.org/format_1930
      inputBinding:
        itemSeparator: ','
        position: 1
        prefix: '-1'
      type: File[]?
    read2:
      format: http://edamontology.org/format_1930
      inputBinding:
        itemSeparator: ','
        position: 2
        prefix: '-2'
      type: File[]?
    singleended:
      format: http://edamontology.org/format_1930
      inputBinding:
        itemSeparator: ','
        position: 4
        prefix: -r
      type: File[]?
    thread-number:
      inputBinding:
        position: 6
        prefix: --num-cpu-threads
      type: int?
  label: 'MEGAHIT: metagenome assembly'
  outputs:
    contigs:
      format: http://edamontology.org/format_1929
      outputBinding:
        glob: megahit_out/final.contigs.fa
      type: File
- baseCommand: runMetaBat.sh
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/metabat
  id: '#metabat.cwl'
  inputs:
    bamfiles:
      inputBinding:
        position: 8
      type: File[]
    contigs:
      inputBinding:
        position: 7
      type: File
    min-class-size:
      inputBinding:
        position: 5
        prefix: --minClsSize
      type: int?
    min-contig-length:
      inputBinding:
        position: 4
        prefix: --minContig
      type: int?
    save-class:
      default: true
      inputBinding:
        position: 3
        prefix: --saveCls
      type: boolean?
    save-unbinned:
      default: false
      inputBinding:
        position: 2
        prefix: --unbinned
      type: boolean?
    suppressBinOutput:
      default: true
      inputBinding:
        position: 6
        prefix: --noBinOut
      type: boolean
    thread-number:
      inputBinding:
        position: 1
        prefix: --numThreads
      type: int?
  label: 'MetaBAT: Metagenome Binning based on Abundance and Tetranucleotide frequency'
  outputs:
    binAssignment:
      format: http://edamontology.org/format_3475
      outputBinding:
        glob: $(inputs.contigs.basename).metabat-bins/bin
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 1
    prefix: -p
    valueFrom: meta
  - position: 2
    prefix: -f
    valueFrom: gff
  - position: 3
    prefix: -o
    valueFrom: "${\n  return inputs.inputFile.nameroot + \".gff\"\n}\n"
  - position: 4
    prefix: -a
    valueFrom: "${\n  return inputs.inputFile.nameroot + \".faa\"\n}\n"
  - position: 5
    prefix: -d
    valueFrom: "${\n  return inputs.inputFile.nameroot + \".fna\"\n}\n"
  baseCommand: prodigal
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/prodigal
  id: '#prodigal.cwl'
  inputs:
    inputFile:
      inputBinding:
        position: 6
        prefix: -i
      type: File
  label: Prodigal 2.6.3
  outputs:
    annotations:
      format: http://edamontology.org/format_2306
      outputBinding:
        glob: $(inputs.inputFile.nameroot + ".gff")
      type: File
    genes:
      format: http://edamontology.org/format_1929
      outputBinding:
        glob: $(inputs.inputFile.nameroot + ".fna")
      type: File
    proteins:
      format: http://edamontology.org/format_1929
      outputBinding:
        glob: $(inputs.inputFile.nameroot + ".faa")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- baseCommand: 'true'
  class: CommandLineTool
  id: '#renamefile.cwl'
  inputs:
  - format: http://edamontology.org/format_3475
    id: '#renamefile.cwl/srcfile'
    type: File
  - id: '#renamefile.cwl/newname'
    type: string
  outputs:
  - format: http://edamontology.org/format_3475
    id: '#renamefile.cwl/outfile'
    outputBinding:
      glob: $(inputs.newname + inputs.srcfile.nameext)
    type: File
  requirements:
    InitialWorkDirRequirement:
      listing:
      - entry: $(inputs.srcfile)
        entryname: $(inputs.newname + inputs.srcfile.nameext)
- baseCommand:
  - samtools
  - merge
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/samtools
  id: '#samtools-merge.cwl'
  inputs:
    inputs:
      format: http://edamontology.org/format_2572
      inputBinding:
        position: 3
      type: File[]
    outFile:
      default: merged.bam
      inputBinding:
        position: 2
      type: string
    thread-number:
      default: 10
      inputBinding:
        position: 1
        prefix: -@
      type: int?
  outputs:
    output:
      format: http://edamontology.org/format_2572
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
    DockerRequirement:
      dockerPull: sjaenick/samtools
  id: '#samtools-sam2bam.cwl'
  inputs:
    fastcompression:
      default: false
      inputBinding:
        position: 3
        prefix: '-1'
      type: boolean
    input:
      inputBinding:
        position: 5
      type: File
    thread-number:
      default: 10
      inputBinding:
        position: 2
        prefix: -@
      type: int?
    uncompressed:
      default: false
      inputBinding:
        position: 1
        prefix: -u
      type: boolean
  outputs:
    output:
      outputBinding:
        glob: $(inputs.input.nameroot + ".bam")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- arguments:
  - position: 2
    prefix: -o
    valueFrom: "${\n  return inputs.input.nameroot + \".bam\"\n}\n"
  baseCommand:
  - samtools
  - sort
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/samtools
  id: '#samtools-sort.cwl'
  inputs:
    input:
      inputBinding:
        position: 3
      type: File
    thread-number:
      default: 10
      inputBinding:
        position: 1
        prefix: -@
      type: int?
  outputs:
    output:
      format: http://edamontology.org/format_2572
      outputBinding:
        glob: $(inputs.input.nameroot + ".bam")
      type: File
  requirements:
  - class: InlineJavascriptRequirement
- baseCommand:
  - SeqRunFetcher
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/mgxannotate
  id: '#seqrunfetch.cwl'
  inputs:
    apiKey:
      inputBinding:
        position: 2
        prefix: -a
      type: string
    hostURI:
      inputBinding:
        position: 1
        prefix: -h
      type: string
    projectName:
      inputBinding:
        position: 3
        prefix: -p
      type: string
    runId:
      inputBinding:
        position: 5
        prefix: -r
      type: string
  label: MGX Fetch sequences
  outputs:
    fwdReads:
      format: http://edamontology.org/format_1930
      outputBinding:
        glob: $(inputs.runId + "_R1.fq")
      type: File
    revReads:
      format: http://edamontology.org/format_1930
      outputBinding:
        glob: $(inputs.runId + "_R2.fq")
      type: File
- baseCommand: tsv2bins.pl
  class: CommandLineTool
  hints:
    DockerRequirement:
      dockerPull: sjaenick/smallscripts
  id: '#tsv2bins.cwl'
  inputs:
    assembledContigs:
      format: http://edamontology.org/format_1929
      inputBinding:
        position: 2
      type: File
    scaffold2bin:
      format: http://edamontology.org/format_3475
      inputBinding:
        position: 1
      type: File
  label: TSV to binned FASTA
  outputs:
    binFastas:
      format: http://edamontology.org/format_1929
      outputBinding:
        glob: '*.fas'
      type: File[]
  requirements:
  - class: InlineJavascriptRequirement
cwlVersion: v1.0
