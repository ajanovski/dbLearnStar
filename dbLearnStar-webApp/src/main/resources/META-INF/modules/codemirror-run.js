/*******************************************************************************
 * Copyright (C) 2021 Vangel V. Ajanovski
 *     
 * This file is part of the dbLearnStar system (hereinafter: dbLearn*).
 *     
 * dbLearn* is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 *     
 * dbLearn* is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
 * details.
 *     
 * You should have received a copy of the GNU General Public License along 
 * with dbLearn*.  If not, see <https://www.gnu.org/licenses/>.
 * 
 ******************************************************************************/

define(["jquery", "t5/core/ajax"], function($, ajax) {

    function codeMirrorStart(_theme) {
        window.editor = CodeMirror
            .fromTextArea(
                document.getElementById('editorarea'),
                {
                    mode: 'text/x-pgsql',
                    indentWithTabs: true,
                    smartIndent: true,
                    lineNumbers: true,
                    matchBrackets: true,
                    theme: _theme,
                    autofocus: true,
                    completeSingle: true,
                    extraKeys: {
                        "Ctrl-Space": "autocomplete",
                    },
                    hintOptions: {
                        tables: {
                            aktivnosti: ["a_id", "a_naslov", "a_opis", "a_procenka_chasovi_rabota", "a_glavna_aktivnost_id", "p_id"],
                            apteka: ["aptid", "aptime", "aptadresa"],
                            apteki: ["a_id", "aime"],
                            apteki_lokacii: ["a_id", "a_reden_br", "adresa", "tel_broj"],
                            apttelbroj: ["aptid", "telbroj"],
                            asistentiistrazhuvachi: ["br_chovek_stud", "br_chovek_prof", "br_proekt"],
                            banki: ["banka_id", "b_naziv", "b_adresa", "grad_id"],
                            banki: ["banka_id", "b_naziv", "b_adresa", "grad_id"],
                            bilshef: ["br_oddel", "datum_od", "br_chovek"],
                            chovek: ["lid", "lime"],
                            davapari: ["br_proekt", "br_sponzor"],
                            dogovor: ["fkomid", "aptid", "lid", "datumpoc", "datumkraj", "sodrzhina"],
                            dogovori: ["fk_id", "a_id", "datum_skl", "l_id", "datum_ist", "sodrzhina"],
                            doktori: ["l_id", "rab_iskustvo", "specijalnost"],
                            doktor: ["lid", "docmbr", "rabiskustvo", "specijalnost"],
                            edinici: ["eid", "eime"],
                            ekspozituri: ["banka_id", "eksp_br", "e_naziv", "e_adresa", "grad_id"],
                            ekvivalencii: ["predmet_pid", "ekvivalenten_na_pid"],
                            evaluacii: ["l_id", "z_reden_broj", "e_reden_broj", "e_naslov", "e_opis", "e_ocenka", "e_izvrshil_l_id", "datum_ocenuvanje"],
                            farmakomi_broevi: ["fk_id", "fk_reden_br", "tel_broj"],
                            farmakomi: ["fk_id", "fk_ime"],
                            fkompanija: ["fkomid", "fkomime"],
                            fkomtelbroj: ["fkomid", "telbroj"],
                            formiraniod: ["eid", "nid"],
                            gradovi: ["grad_id", "g_naziv"],
                            ima: ["kod", "oid"],
                            istrazhuvachi: ["br_chovek", "br_proekt"],
                            lek: ["lekid", "lekime", "sostav"],
                            lekovi: ["fk_id", "prod_id", "l_ime", "sostav"],
                            listaizbrani: ["sid", "semestar", "pid", "ocenka"],
                            lugje: ["identifikator", "ime", "adresa", "br_chovek", "embg", "ime_chovek", "vozrast"],
                            lugje: ["l_id", "ime", "adresa", "embg", "drzhava"],
                            lugje: ["l_id", "ime", "prezime", "email", "datum_ragjanje"],
                            lugje: ["l_id", "ime", "prezime", "embg", "datum_ragj"],
                            matichen: ["liddok", "lidpac", "mbroj"],
                            matichen_na: ["p_l_id", "m_datum_poch", "m_datum_kraj", "d_l_id"],
                            mentori: ["midentifikator"],
                            mesta: ["m_id", "m_ime"],
                            mozhe_da_se_natprevaruva: ["l_id", "o_id"],
                            nasoki: ["nid", "nime", "nmaxkrediti", "vkupnokreditidiplomiranje"],
                            nastavnici: ["l_id"],
                            nastavnici: ["nidentifikator", "uchid"],
                            natprevari: ["kod", "mesec", "opseg", "rang", "naziv"],
                            oblasti: ["br_oblast", "ime_oblast"],
                            oblasti: ["o_id", "o_naziv"],
                            oblasti: ["oid", "onaziv"],
                            oblast_na_natprevar: ["kod", "o_id"],
                            oceneto: ["kod", "m_id", "datum", "red_broj", "l_id", "ocenuvach_l_id"],
                            ocenuvachi: ["l_id"],
                            ocenuvachi: ["oidentifikator"],
                            ocenuva: ["kod", "data", "oidentifikator"],
                            ocenuvale: ["kod", "m_id", "datum", "l_id"],
                            oddeli: ["br_oddel", "ime_oddel", "br_chovek", "br_oblast"],
                            odgovorni_lica: ["smetka_br", "trans_br", "banka_id", "vrab_br"],
                            osvoeni: ["kod", "data", "uidentifikator", "midentifikator", "oidentifikator", "mesto", "bodovi"],
                            pacienti: ["l_id", "p_adresa"],
                            pacient: ["lid", "pacmbr", "pacadresa", "datumragj"],
                            podoblasti_nadoblasti: ["o_id", "nadoblast_o_id"],
                            predmeti: ["pid", "pime", "pkrediti"],
                            prijavnilistovi: ["sid", "semestar", "status"],
                            prodava: ["fk_id", "prod_id", "a_id", "cena"],
                            prodavaprodukt: ["aptid", "fkomid", "prodid", "cena"],
                            produkt: ["fkomid", "prodid", "maxcena", "lekid", "prodime"],
                            proekti: ["br_proekt", "ime_proekt", "br_chovek"],
                            proekti: ["p_id", "p_naslov", "p_opis"],
                            profesori: ["br_chovek", "specijalnost", "titula"],
                            rabotel_vo: ["l_id", "u_id"],
                            raboti: ["br_chovek", "br_oddel", "procent"],
                            raboti_kako: ["banka_id", "vrab_br", "rabmesto_id"],
                            rabotni_mesta: ["rabmesto_id", "rm_naziv"],
                            realizacii: ["kod", "data", "mesto"],
                            realizacii: ["kod", "m_id", "datum"],
                            recepti: ["p_l_id", "d_l_id", "datum_izd", "fk_id", "prod_id", "doza"],
                            recept: ["recid", "datum", "doza", "liddok", "lidpac", "mbroj", "lekid"],
                            registracii: ["kod", "m_id", "datum", "red_broj", "u_id"],
                            shefovi: ["br_oddel", "datum_od", "datum_do"],
                            sponzori: ["br_sponzor", "ime_sponzor"],
                            studenti: ["br_chovek", "vid_studii", "br_chovek_sovetnik", "br_oddel"],
                            studenti: ["sid", "sindeks", "sime", "semail", "matichna_eid"],
                            supervizori: ["l_id"],
                            supervizor: ["lid"],
                            uchenici: ["l_id"],
                            uchenici: ["uidentifikator", "uchid"],
                            uchestva_na_uchenici: ["kod", "m_id", "datum", "red_broj", "l_id", "ostvareni_bodovi", "osvoeno_mesto"],
                            uchestvuval: ["kod", "data", "uchid", "uidentifikator", "midentifikator"],
                            uchestvuval_vo: ["l_id", "p_id", "uloga"],
                            uchilishta: ["uchid", "uchnaziv", "uchadresa"],
                            uchilishta: ["u_id", "u_naziv", "l_id"],
                            upisi: ["sid", "redbr", "semestar_od", "semestar_do", "zapishan_na_nasoka_nid"],
                            vraboteni: ["banka_id", "vrab_br", "v_embg", "v_ime", "godina_vrab"],
                            zadolzhitelni: ["nid", "pid"],
                            zakazhano: ["kod", "data", "uchid"],
                            zapisnici: ["l_id", "z_reden_broj", "z_naslov", "z_opis", "z_rabotel_chasovi", "a_id", "datum_podnesen"],
                            znae: ["uidentifikator", "oid"],
                        }
                    }
                });
    }

    function initialize(initialTheme) {
        $(document).ready(codeMirrorStart(initialTheme));
    }

    $('select.selectTheme').on("change", function() {
        var str = "";
        $("select.selectTheme option:selected").each(function() {
            str = $(this).text();
        });
        ajax("changeTheme", {
            element: null,
            data: {
                "newTheme": str
            },
            method: "POST"
        });
        window.editor.setOption("theme", str);
    });

    return { initialize: initialize };

});
