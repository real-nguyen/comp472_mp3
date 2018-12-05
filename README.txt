Basic setup:
- Training corpora are in input/training folder
- Sentences to read are in input/sentences.txt
- Language model outputs are in output folder
- Sentence analysis outputs are in output/sentences folder
- probTable text files are bigram count tables. For example, the first table cell in probTableFR.txt corresponds to the bigram "aa", which appears 182 times in the FR corpus -> 182.5 with 0.5 smoothing. Open in Notepad++ for best results.

Experiments
- Sentences to read for experiments 1-3 are in input/experiments folder
- Training corpus for experiment 3 is in input/experiments/trainEX.txt
- Language model outputs for experiment 3 is in output/experiments
- Sentence outputs for all experiments are in output/experiments/sentences. The exn- prefix represents the experiment the output belongs to, where n is the experiment number. For example, ex1-out1.txt is the output for the first sentence of experiment 1.