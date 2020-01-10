#!/bin/perl
#use strict;
use warnings;
use JSON::MaybeXS ();

%species_info = (
    # # Actinidia chinensis (Ac/achi) - Ensembl
             'achi' => {'name' => ['Actinidia chinensis'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Actinidia_chinensis_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Panicum hallii (Ph/phal) - Ensembl
             'phal' => {'name' => ['Panicum hallii'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Panicum_hallii_fil2_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Panicum hallii var. hallii (Phh/phvh) - Ensembl
             'phvh' => {'name' => ['Panicum halliivarhallii'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_panicum_hallii_hal2_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Arabidopsis halleri (Ah/ahal) - Ensembl
             'ahal' => {'name' => ['Arabidopsis halleri'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Arabidopsis_halleri_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Triticum dicoccoides (Td/tdic) - Ensembl
             'tdic' => {'name' => ['Triticum dicoccoides'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Triticum_dicoccoides_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Vigna radiata (Vr/vrad) - Ensembl
             'vrad' => {'name' => ['Vigna radiata'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Vigna_radiata_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Vigna angularis (Va/vang) - Ensembl
             'vang' => {'name' => ['Vigna angularis'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Vigna_angularis_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Daucus carota (Dc/dcar) - Ensembl
             'dcar' => {'name' => ['Daucus carota'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Daucus_carota_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

        # # Synechocystis pcc6803 (Sp/spcc) - MicrobeDB
                 'spcc' => {'name' => ['Synechocystis pcc6803'],
                            'alt_refdb' => {'dbname' => ['MicrobeDB'],
                                        'url' => 'http://cyano.genome.jp',
                                        'access' => 'http://cyano.genome.jp/cgi-bin/cyorf_view.cgi?ORG=syn&ACCESSION=###ID###'}},

    # # Beta vulgaris (Bv/bvul) - Ensembl
             'bvul' => {'name' => ['Beta vulgaris'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Beta_vulgaris_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Brassica napus (Bn/bnap) - Ensembl
             'bnap' => {'name' => ['Brassica napus'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Brassica_napus_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Trifolium pratense (Tp/tpra) - Ensembl
             'tpra' => {'name' => ['Trifolium pratense'],
                        'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Trifolium_pratense_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Zea mays (Zm/zmay) - MaizeGDB / Ensembl
        'zmay' => {'name' => ['Zea mays'],
                'alt_refdb' => {'dbname' => ['MaizeGDB'],
                        'url' => 'http://maizegdb.org/',
                        'access' => 'http://www.maizegdb.org/gene_center/gene/###ID###'},
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Zea_mays_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Jatropha curcas (Jc/jcur) - KDRI_Jc
        'jcur' => {'name' => ['Jatropha curcas'],
                'alt_refdb' => {'dbname' => ['KDRI_Jc'],
                    'url' => 'http://www.kazusa.or.jp/jatropha/',
                    'access' => 'http://www.kazusa.or.jp/jatropha/cgi-bin/orfinfo.cgi?db=jatropha&id=###ID###'}},

    # # Eucalyptus grandis (Eg/egra) - Phytozome_Eg
        'egra' => {'name' => ['Eucalyptus grandis'],
                'alt_refdb' => {'dbname' => ['Phytozome_Eg'],
                    'url' => 'http://phytozome.jgi.doe.gov/',
                    'access' => 'http://phytozome.jgi.doe.gov/pz/portal.html#!results?search=0&crown=1&star=1&method=4908&searchText=###ID###'}},

    # # Mimulus guttatus (Mg/mgut) - Phytozome_Mg
        'mgut' => {'name' => ['Mimulus guttatus'],
                'alt_refdb' => {'dbname' => ['Phytozome_Mg'],
                    'url' => 'http://phytozome.jgi.doe.gov/',
                    'access' => 'http://phytozome.jgi.doe.gov/pz/portal.html#!results?search=0&crown=1&star=1&method=3464&searchText=###ID###'}},

    # # Citrus sinensis (Cs/csin) - Phytozome_Cs
        'csin' => {'name' => ['Citrus sinensis'],
                'alt_refdb' => {'dbname' => ['Phytozome_Cs'],
                    'url' => 'http://phytozome.jgi.doe.gov/',
                    'access' => 'http://phytozome.jgi.doe.gov/pz/portal.html#!results?search=0&crown=1&star=1&method=2312&searchText=###ID###'}},

    # # Malus domestica (Md/mdom) - Phytozome_Md
        'mdom' => {'name' => ['Malus domestica'],
                'alt_refdb' => {'dbname' => ['Phytozome_Md'],
                    'url' => 'http://phytozome.jgi.doe.gov/',
                    'access' => 'http://phytozome.jgi.doe.gov/pz/portal.html#!results?search=0&crown=1&star=1&method=3124&searchText=###ID###'}},

    # # Fragaria vesca (Fv/fves) - Phytozome_Fv
        'fves' => {'name' => ['Fragaria vesca'],
                'alt_refdb' => {'dbname' => ['Phytozome_Fv'],
                    'url' => 'http://phytozome.jgi.doe.gov/',
                    'access' => 'http://phytozome.jgi.doe.gov/pz/portal.html#!results?search=0&crown=1&star=1&method=3309&searchText=###ID###'}},

    # # Cajanus cajan (Cc/ccaj) - LegumeInfo   (down)
        'ccaj' => {'name' => ['Cajanus cajan'],
                'alt_refdb' => {'dbname' => ['LegumeInfo'],
                    'url' => 'https://legumeinfo.org/',
                    'access' => 'https://legumeinfo.org/feature/Cajanus/cajan/gene/###ID###_gene'}},

    # # Cicer arietinum (Ca/cari) - NCBI
        'cari' => {'name' => ['Cicer arietinum'],
                'alt_refdb' => {'dbname' => ['NCBI'],
                    'url' => 'http://www.ncbi.nlm.nih.gov/',
                    'access' => 'http://www.ncbi.nlm.nih.gov/protein/###ID###'}},

    # # Coffea canephora (Cca/ccan) - Jaiswal
        'ccan' => {'name' => ['Coffea canephora'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    # # Capsicum annuum (Can/cann) - Jaiswal
        'cann' => {'name' => ['Capsicum annuum'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    #
    # # Arachis duranensis (Ad/adur) - PeanutBase_Ad
        'adur' => {'name' => ['Arachis duranensis'],
                'alt_refdb' => {'dbname' => ['PeanutBase_Ad'],
                    'url' => 'http://peanutbase.org/',
                    'access' => 'http://peanutbase.org/feature/Arachis/duranensis/gene/###ID###'}},

    # # Arachis ipaensis (Ai/aipa) - PeanutBase_Ai
        'aipa' => {'name' => ['Arachis ipaensis'],
                'alt_refdb' => {'dbname' => ['PeanutBase_Ai'],
                    'url' => 'http://peanutbase.org/',
                    'access' => 'http://peanutbase.org/feature/Arachis/ipaensis/gene/###ID###'}},

    #
    # # Cucumis sativus (Csa/csat) - Ensembl
        'csat' => {'name' => ['Cucumis sativus'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Cucumis_sativus_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Chondrus crispus (Ccr/ccri) - Ensembl
        'ccri' => {'name' => ['Chondrus crispus'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Chondrus_crispus_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Corchorus capsularis (Ccp/ccap) - Ensembl
        'ccap' => {'name' => ['Corchorus capsularis'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Corchorus_capsularis_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Dioscorea rotundata (Dr/drot) - Ensembl
        'drot' => {'name' => ['Dioscorea rotundata'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Dioscorea_rotundata_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Galdieria sulphuraria (Gs/gsul) - Ensembl
        'gsul' => {'name' => ['Galdieria sulphuraria'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Galdieria_sulphuraria_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Helianthus annuus (Ha/hann) - Ensembl
        'hann' => {'name' => ['Helianthus annuus'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Helianthus_annuus_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Lupinus angustifolius (La/lang) - Ensembl
        'lang' => {'name' => ['Lupinus angustifolius'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Lupinus_angustifolius_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Nicotiana attenuata (Na/natt) - Ensembl
        'natt' => {'name' => ['Nicotiana attenuata'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Nicotiana_attenuata_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Gossypium raimondii (Gr/grai) - Ensembl
        'grai' => {'name' => ['Gossypium raimondii'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Gossypium_raimondii_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Manihot esculenta (Me/mesc) - Ensembl
        'mesc' => {'name' => ['Manihot esculenta'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Manihot_esculenta_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # #  Phaseolus vulgaris (Pv/pvul) - Ensembl
        'pvul' => {'name' => ['Phaseolus vulgaris'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Phaseolus_vulgaris_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
    
    # # Arabidopsis lyrata (Al/alyr) - Ensembl
        'alyr' => {'name' => ['Arabidopsis lyrata'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Arabidopsis_lyrata_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Brassica oleracea (Bo/bole) - Ensembl
        'bole' => {'name' => ['Brassica oleracea'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Brassica_oleracea_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Theobroma cacao (Tc/tcac) - Ensembl
        'tcac' => {'name' => ['Theobroma cacao'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Theobroma_cacao_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    #
    # # Arabidopsis thaliana (At/atha) - TAIR / Ensembl
        'atha' => {'name' => ['Arabidopsis thaliana'],
                    'alt_refdb' => {'dbname' => ['TAIR'],
                    'url' => 'http://www.arabidopsis.org',
                    'access' => 'http://www.arabidopsis.org/servlets/TairObject?type=locus&name=###ID###'},
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Arabidopsis_thaliana_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Medicago truncatula (Mt/mtru) - Ensembl
        'mtru' => {'name' => ['Medicago truncatula'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Medicago_truncatula_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Glycine max (Gm/gmax) - Ensembl
        'gmax' => {'name' => ['Glycine max'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Glycine_max_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Solanum lycopersicum (Sl/slyc) - Ensembl
        'slyc' => {'name' => ['Solanum lycopersicum'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Solanum_lycopersicum_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Solanum tuberosum (St/stub) - Ensembl
        'stub' => {'name' => ['Solanum tuberosum'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Solanum_tuberosum_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Populus trichocarpa (Pt/ptri) - Ensembl
        'ptri' => {'name' => ['Populus trichocarpa'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Populus_trichocarpa_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Prunus persica (Pp/pper) - Ensembl
        'pper' => {'name' => ['Prunus persica'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Prunus_persica_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Brassica rapa (Br/brap) - Ensembl
        'brap' => {'name' => ['Brassica rapa'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Brassica_rapa_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Vitis vinifera (Vv/vvin) - Ensembl
        'vvin' => {'name' => ['Vitis vinifera'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Vitis_vinifera_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    #
    # # Phoenix dactylifera (Pd/pdac) - Jaiswal
        'pdac' => {'name' => ['Phoenix dactylifera'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    # # Triticum turgidum (Tt/ttur) - Jaiswal
        'ttur' => {'name' => ['Triticum turgidum'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    #
    # # Triticum aestivum (Ta/taes) - Ensembl
        'taes' => {'name' => ['Triticum aestivum'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Triticum_aestivum_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Musa acuminata (Ma/macu) - Ensembl
        'macu' => {'name' => ['Musa acuminata'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Musa_acuminata_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Setaria italica (Si/sita) - Ensembl
        'sita' => {'name' => ['Setaria italica'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Setaria_italica_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Sorghum bicolor (Sb/sbic) - Ensembl
        'sbic' => {'name' => ['Sorghum bicolor'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Sorghum_bicolor_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Leersia perrieri (Lp/lper) - Ensembl
        'lper' => {'name' => ['Leersia perrieri'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Leersia_perrieri_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Brachypodium distachyon (Bd/bdis) - Ensembl
        'bdis' => {'name' => ['Brachypodium distachyon'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Brachypodium_distachyon_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Triticum urartu (Tu/tura) - Ensembl
        'tura' => {'name' => ['Triticum urartu'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Triticum_urartu_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Aegilops tauschii (Ata/atau) - Ensembl
        'atau' => {'name' => ['Aegilops tauschii'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Aegilops_tauschii_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Hordeum vulgare (Hv/hvul) - Ensembl
        'hvul' => {'name' => ['Hordeum vulgare'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Hordeum_vulgare_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    #
    # # Picea abies (Pa/pabi) - Congenie
        'pabi' => {'name' => ['Picea abies'],
                'alt_refdb' => {'dbname' => ['Congenie'],
                    'url' => 'http://congenie.org/',
                    'access' => 'http://congenie.org/gene?id=###ID###'}},

    # # Pinus taeda (Pta/ptae) - Jaiswal
        'ptae' => {'name' => ['Pinus taeda'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    #
    # # Amborella trichopoda (Atr/atri) - Ensembl
        'atri' => {'name' => ['Amborella trichopoda'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Amborella_trichopoda_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    #
    # # Physcomitrella patens (Ppa/ppat) - Ensembl
        'ppat' => {'name' => ['Physcomitrella patens'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Physcomitrella_patens_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Selaginella moellendorffii (Sm/smoe) - Ensembl
        'smoe' => {'name' => ['Selaginella moellendorffii'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Selaginella_moellendorffii_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Chlamydomonas reinhardtii (Cr/crei) - Ensembl
        'crei' => {'name' => ['Chlamydomonas reinhardtii'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Chlamydomonas_reinhardtii_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Cyanidioschyzon merolae (Cm/cmer) - Ensembl
        'cmer' => {'name' => ['Cyanidioschyzon merolae'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Cyanidioschyzon_merolae_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Ostreococcus lucimarinus (Olu/oluc) - Ensembl
        'oluc' => {'name' => ['Ostreococcus lucimarinus'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Ostreococcus_lucimarinus_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    #
    # # Oryza australiensis (Oa/oaus) - Jaiswal
        'oaus' => {'name' => ['Oryza australiensis'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    # # Oryza granulata (Ogr/ogra) - Jaiswal
        'ogra' => {'name' => ['Oryza granulata'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    # # Oryza kasalath (Ok/okas) - Jaiswal
        'okas' => {'name' => ['Oryza kasalath'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    # # Oryza minuta (Omi/omin) - Jaiswal
        'omin' => {'name' => ['Oryza minuta'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    # # Oryza officinalis (Oo/ooff) - Jaiswal
        'ooff' => {'name' => ['Oryza officinalis'],
               'alt_refdb' => {'dbname' => ['Jaiswal'],
                        'url' => 'http://jaiswallab.cgrb.oregonstate.edu/',
                    'access' => '/pages/content/release-summary/'}},

    #
    # # Oryza longistaminata (Ol/olon) - Ensembl
                'olon' => {'name' => ['Oryza longistaminata'],
                            'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_longistaminata_PROTEIN'],
                                    'url' => 'http://ensembl.gramene.org',
                                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                                'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza meridionalis (Ome/omer) - Ensembl
        'omer' => {'name' => ['Oryza meridionalis'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_meridionalis_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza barthii (Oba/obar) - Ensembl
        'obar' => {'name' => ['Oryza barthii'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_barthii_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza nivara (On/oniv) - Ensembl
        'oniv' => {'name' => ['Oryza nivara'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_nivara_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza brachyantha (Obr/obra) - Ensembl
        'obra' => {'name' => ['Oryza brachyantha'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_brachyantha_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza glaberrima (Ogla/ogla) - Ensembl
        'ogla' => {'name' => ['Oryza glaberrima'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_glaberrima_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza punctata (Op/opun) - Ensembl
        'opun' => {'name' => ['Oryza punctata'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_punctata_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza glumaepatula (Oglu/oglu) - Ensembl
        'oglu' => {'name' => ['Oryza glumaepatula'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_glumaepatula_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza rufipogon (Or/oruf) - Ensembl
        'oruf' => {'name' => ['Oryza rufipogon'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_rufipogon_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza sativa subsp. indica (Osi/osai) - Ensembl
        'osai' => {'name' => ['Oryza sativa subsp. Indica'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_sativa_Indica_Group_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},

    # # Oryza sativa (reference) - UniProt / Ensembl
        'osat' => {'name' => ['Oryza sativa'],
                'refdb' => {'dbname' => ['ENSEMBL', 'Ensembl', 'ENSEMBL_Oryza_sativa_PROTEIN'],
                    'url' => 'http://ensembl.gramene.org',
                    'access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###',
                    'ensg_access' => 'http://ensembl.gramene.org/Gene/Summary?g=###ID###'}},
);

my $json_parser = JSON::MaybeXS->new(utf8 => 1, pretty => 1, sort_by => 1);
my $species_json = $json_parser->encode({%species_info});
print $species_json;