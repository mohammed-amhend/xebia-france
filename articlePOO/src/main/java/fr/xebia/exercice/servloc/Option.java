/*
 * Copyright 2008-2009 Xebia and the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.xebia.exercice.servloc;

import java.util.Date;

class Option implements Titre {

    final Integer id;
    final Date dateExercice;
    final double prixExercice;
    final Action sousJacent;

    public Option(Integer id, double prixExercice, Date dateExercice, Action sousJacent) {
        this.id = id;
        this.prixExercice = prixExercice;
        this.dateExercice = dateExercice;
        this.sousJacent = sousJacent;
    }

    @Override
    public double valorise() {

        double vol = ServiceLocator.getBean(ServLocMarketdataRepository.class).getVolatilite(sousJacent.id);
        double taux = ServiceLocator.getBean(ServLocMarketdataRepository.class).getTaux(this.id);
        return valorise(dateExercice, prixExercice, sousJacent, vol, taux);
    }

    private double valorise(Date dateExercice, double prixExercice, Action sousJacent, double vol, double taux) {
        // implementation de la valo
        return 0;
    }
}
